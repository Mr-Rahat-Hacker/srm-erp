package com.srm.erp.sap.service;

import com.srm.erp.sap.api.SapIntegrationRecord;
import com.srm.erp.sap.api.SapPirRequestCreate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SapPirOutboxService {
    private static final String OPERATION = "PIR_UPSERT";
    private final JdbcTemplate jdbc;

    public SapPirOutboxService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public SapIntegrationRecord create(UUID awardId, UUID buyerId, SapPirRequestCreate request) {
        if (request == null || request.sapSystemCode() == null || request.sapSystemCode().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose the configured SAP system");
        }
        AwardContext award = jdbc.query("""
                SELECT a.award_id, a.supplier_id, a.quotation_version_id, r.rfq_id,
                       r.rfq_number, r.currency_code, r.plant_code, r.purchasing_group,
                       v.version_number, v.currency_code AS quote_currency, v.payment_terms,
                       v.incoterms, v.total_landed_cost
                FROM srm.award a
                JOIN srm.rfq r ON r.rfq_id = a.rfq_id
                JOIN srm.approval_request ar ON ar.approval_request_id = a.approval_request_id
                JOIN srm.quotation_version v ON v.quotation_version_id = a.quotation_version_id
                WHERE a.award_id = ? AND r.buyer_user_id = ?
                  AND a.award_status = 'APPROVED' AND ar.status = 'APPROVED'
                FOR UPDATE OF a
                """, rs -> rs.next() ? mapAward(rs) : null, awardId, buyerId);
        if (award == null) throw new ResponseStatusException(NOT_FOUND, "Approved award not found");

        SapMapping mapping = jdbc.query("""
                SELECT sap_system_code, sap_vendor_code
                FROM srm.sap_vendor_mapping
                WHERE supplier_id = ? AND sap_system_code = ? AND is_active = true
                """, rs -> rs.next() ? new SapMapping(rs.getString("sap_system_code"),
                rs.getString("sap_vendor_code")) : null, award.supplierId(), request.sapSystemCode().trim());
        if (mapping == null) {
            throw new ResponseStatusException(BAD_REQUEST, "An active SAP Vendor Code mapping is required for this system");
        }
        SapIntegrationRecord existing = findForAward(awardId);
        if (existing != null) return existing;

        UUID requestKey = UUID.randomUUID();
        String payload = jdbc.queryForObject("""
                SELECT jsonb_build_object(
                    'requestKey', ?::text,
                    'operation', 'UPSERT_PURCHASE_INFO_RECORD',
                    'sapSystemCode', ?,
                    'awardId', ?::text,
                    'rfqNumber', ?,
                    'supplierId', ?::text,
                    'vendorCode', ?,
                    'purchasingGroup', ?,
                    'plantCode', ?,
                    'quotationVersion', ?,
                    'currencyCode', ?,
                    'totalLandedCost', ?,
                    'paymentTerms', ?,
                    'incoterms', ?,
                    'items', (
                        SELECT jsonb_agg(jsonb_build_object(
                            'lineNumber', rl.line_number,
                            'materialCode', rl.sap_material_code,
                            'description', rl.description,
                            'quantity', rl.quantity,
                            'unit', rl.uom_code,
                            'netUnitPrice', ql.net_unit_price,
                            'landedLineTotal', ql.landed_line_total
                        ) ORDER BY rl.line_number)
                        FROM srm.quotation_line ql
                        JOIN srm.rfq_line rl ON rl.rfq_line_id = ql.rfq_line_id
                        WHERE ql.quotation_version_id = ?
                    )
                )::text
                """, String.class, requestKey, mapping.systemCode(), award.awardId(), award.rfqNumber(),
                award.supplierId(), mapping.vendorCode(), award.purchasingGroup(), award.plantCode(),
                award.versionNumber(), award.quoteCurrency(), award.totalLandedCost(), award.paymentTerms(),
                award.incoterms(), award.quotationVersionId());
        UUID eventId = jdbc.queryForObject("""
                INSERT INTO srm.sap_integration_event
                    (supplier_id, award_id, operation_type, status, request_key, request_payload)
                VALUES (?, ?, ?, 'QUEUED', ?, CAST(? AS jsonb))
                RETURNING integration_event_id
                """, UUID.class, award.supplierId(), award.awardId(), OPERATION, requestKey, payload);
        audit(buyerId, "SAP_PIR_REQUEST_QUEUED", eventId, award.rfqId(), "SUCCESS", requestKey.toString());
        return find(eventId);
    }

    public List<SapIntegrationRecord> forAward(UUID awardId, UUID buyerId) {
        Boolean owns = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM srm.award a JOIN srm.rfq r ON r.rfq_id = a.rfq_id
                    WHERE a.award_id = ? AND r.buyer_user_id = ?)
                """, Boolean.class, awardId, buyerId);
        if (!Boolean.TRUE.equals(owns)) throw new ResponseStatusException(NOT_FOUND, "Award not found");
        return jdbc.query("""
                SELECT integration_event_id, request_key, award_id, operation_type, status,
                       external_reference, sanitized_error, attempt_count, created_at, updated_at
                FROM srm.sap_integration_event
                WHERE award_id = ? AND operation_type = ?
                ORDER BY created_at DESC
                """, (rs, row) -> mapRecord(rs), awardId, OPERATION);
    }

    @Transactional
    public SapIntegrationRecord retry(UUID eventId, UUID buyerId) {
        SapIntegrationRecord record = jdbc.query("""
                SELECT e.integration_event_id, e.request_key, e.award_id, e.operation_type, e.status,
                       e.external_reference, e.sanitized_error, e.attempt_count, e.created_at, e.updated_at
                FROM srm.sap_integration_event e
                JOIN srm.award a ON a.award_id = e.award_id
                JOIN srm.rfq r ON r.rfq_id = a.rfq_id
                WHERE e.integration_event_id = ? AND r.buyer_user_id = ?
                FOR UPDATE OF e
                """, rs -> rs.next() ? mapRecord(rs) : null, eventId, buyerId);
        if (record == null) throw new ResponseStatusException(NOT_FOUND, "SAP integration request not found");
        if (!"FAILED".equals(record.status())) {
            throw new ResponseStatusException(CONFLICT, "Only failed SAP requests can be retried");
        }
        jdbc.update("""
                UPDATE srm.sap_integration_event
                SET status = 'QUEUED', sanitized_error = NULL, updated_at = now()
                WHERE integration_event_id = ? AND status = 'FAILED'
                """, eventId);
        audit(buyerId, "SAP_PIR_RETRY_QUEUED", eventId, null, "SUCCESS", record.requestKey().toString());
        return find(eventId);
    }

    private SapIntegrationRecord findForAward(UUID awardId) {
        return jdbc.query("""
                SELECT integration_event_id, request_key, award_id, operation_type, status,
                       external_reference, sanitized_error, attempt_count, created_at, updated_at
                FROM srm.sap_integration_event
                WHERE award_id = ? AND operation_type = ?
                """, rs -> rs.next() ? mapRecord(rs) : null, awardId, OPERATION);
    }

    private SapIntegrationRecord find(UUID eventId) {
        return jdbc.query("""
                SELECT integration_event_id, request_key, award_id, operation_type, status,
                       external_reference, sanitized_error, attempt_count, created_at, updated_at
                FROM srm.sap_integration_event WHERE integration_event_id = ?
                """, rs -> rs.next() ? mapRecord(rs) : null, eventId);
    }

    private void audit(UUID actor, String action, UUID eventId, UUID rfqId, String outcome, String reason) {
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, effective_roles, action_code, entity_type,
                     entity_id, rfq_id, outcome, reason)
                VALUES (?, 'USER', ARRAY['PROCUREMENT_BUYER'], ?, 'SAP_INTEGRATION_EVENT',
                        ?, ?, ?, ?)
                """, actor, action, eventId, rfqId, outcome, reason);
    }

    private AwardContext mapAward(ResultSet rs) throws SQLException {
        return new AwardContext(rs.getObject("award_id", UUID.class),
                rs.getObject("supplier_id", UUID.class), rs.getObject("quotation_version_id", UUID.class),
                rs.getObject("rfq_id", UUID.class), rs.getString("rfq_number"),
                rs.getString("currency_code"), rs.getString("plant_code"),
                rs.getString("purchasing_group"), rs.getInt("version_number"),
                rs.getString("quote_currency"), rs.getString("payment_terms"),
                rs.getString("incoterms"), rs.getBigDecimal("total_landed_cost"));
    }

    private SapIntegrationRecord mapRecord(ResultSet rs) throws SQLException {
        return new SapIntegrationRecord(rs.getObject("integration_event_id", UUID.class),
                rs.getObject("request_key", UUID.class), rs.getObject("award_id", UUID.class),
                rs.getString("operation_type"), rs.getString("status"), rs.getString("external_reference"),
                rs.getString("sanitized_error"), rs.getInt("attempt_count"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private record AwardContext(UUID awardId, UUID supplierId, UUID quotationVersionId, UUID rfqId,
                                String rfqNumber, String currencyCode, String plantCode, String purchasingGroup,
                                int versionNumber, String quoteCurrency, String paymentTerms,
                                String incoterms, java.math.BigDecimal totalLandedCost) {}
    private record SapMapping(String systemCode, String vendorCode) {}
}
