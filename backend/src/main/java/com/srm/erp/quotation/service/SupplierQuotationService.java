package com.srm.erp.quotation.service;

import com.srm.erp.quotation.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SupplierQuotationService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final JdbcTemplate jdbc;

    public SupplierQuotationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SupplierRfqSummary> findInvitedRfqs(UUID userId) {
        return jdbc.query("""
                SELECT r.rfq_id, r.rfq_number, r.title, r.currency_code, r.submission_deadline,
                       i.invitation_status,
                       (SELECT q.lifecycle_status FROM srm.quotation q
                         WHERE q.rfq_id = r.rfq_id AND q.supplier_id = s.supplier_id) AS quotation_status,
                       (SELECT max(v.version_number) FROM srm.quotation q
                         JOIN srm.quotation_version v ON v.quotation_id = q.quotation_id
                         WHERE q.rfq_id = r.rfq_id AND q.supplier_id = s.supplier_id) AS latest_version
                FROM srm.rfq r
                JOIN srm.rfq_invitation i ON i.rfq_id = r.rfq_id
                JOIN srm.supplier_organization s ON s.supplier_id = i.supplier_id
                JOIN srm.supplier_user_membership m ON m.supplier_id = s.supplier_id
                WHERE m.user_id = ? AND m.membership_status = 'ACTIVE'
                  AND s.lifecycle_status IN ('APPROVED', 'ACTIVE')
                  AND i.invitation_status IN ('INVITED', 'ACKNOWLEDGED')
                  AND r.status = 'PUBLISHED'
                ORDER BY r.submission_deadline, r.rfq_number
                """, (rs, row) -> new SupplierRfqSummary(
                rs.getObject("rfq_id", UUID.class), rs.getString("rfq_number"), rs.getString("title"),
                rs.getString("currency_code"), rs.getTimestamp("submission_deadline").toInstant(),
                rs.getString("invitation_status"), rs.getString("quotation_status"),
                (Integer) rs.getObject("latest_version")
        ), userId);
    }

    public SupplierRfqResponse findRfq(UUID rfqId, UUID userId) {
        RfqAccess access = requireAccess(rfqId, userId);
        return response(access, userId);
    }

    @Transactional
    public SupplierRfqResponse saveDraft(UUID rfqId, UUID userId, QuotationDraftRequest request) {
        RfqAccess access = requireAccess(rfqId, userId);
        ensureBeforeDeadline(access);
        validateCurrencyAndLines(access, request);

        jdbc.update("""
                INSERT INTO srm.quotation (rfq_id, supplier_id, lifecycle_status)
                VALUES (?, ?, 'DRAFT')
                ON CONFLICT (rfq_id, supplier_id) DO NOTHING
                """, rfqId, access.supplierId());
        QuotationHeader quotation = jdbc.queryForObject("""
                SELECT quotation_id, lifecycle_status FROM srm.quotation
                WHERE rfq_id = ? AND supplier_id = ? FOR UPDATE
                """, (rs, row) -> new QuotationHeader(
                rs.getObject("quotation_id", UUID.class), rs.getString("lifecycle_status")),
                rfqId, access.supplierId());

        List<VersionHeader> latest = jdbc.query("""
                SELECT quotation_version_id, version_number, version_status
                FROM srm.quotation_version WHERE quotation_id = ?
                ORDER BY version_number DESC LIMIT 1 FOR UPDATE
                """, (rs, row) -> new VersionHeader(
                rs.getObject("quotation_version_id", UUID.class), rs.getInt("version_number"),
                rs.getString("version_status")), quotation.quotationId());

        int versionNumber;
        UUID versionId;
        if (!latest.isEmpty() && "DRAFT".equals(latest.getFirst().status())) {
            VersionHeader draft = latest.getFirst();
            versionNumber = draft.versionNumber();
            versionId = draft.versionId();
            jdbc.update("""
                    UPDATE srm.quotation_version SET currency_code = ?, validity_date = ?,
                        payment_terms = ?, incoterms = ?, lead_time_days = ?, minimum_order_quantity = ?,
                        warranty_terms = ?, commercial_remarks = ?, technical_deviations = ?,
                        total_landed_cost = ?, submitted_by = NULL, submitted_at = NULL
                    WHERE quotation_version_id = ? AND version_status = 'DRAFT'
                    """, access.currencyCode(), request.validityDate(), blankToNull(request.paymentTerms()),
                    blankToNull(request.incoterms()), request.leadTimeDays(), request.minimumOrderQuantity(),
                    blankToNull(request.warrantyTerms()), blankToNull(request.commercialRemarks()),
                    blankToNull(request.technicalDeviations()), calculateTotal(access, request.lines()), versionId);
            jdbc.update("DELETE FROM srm.quotation_line WHERE quotation_version_id = ?", versionId);
        } else {
            versionNumber = latest.isEmpty() ? 1 : latest.getFirst().versionNumber() + 1;
            versionId = jdbc.queryForObject("""
                    INSERT INTO srm.quotation_version
                        (quotation_id, version_number, currency_code, validity_date, payment_terms,
                         incoterms, lead_time_days, minimum_order_quantity, warranty_terms,
                         commercial_remarks, technical_deviations, total_landed_cost, version_status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                    RETURNING quotation_version_id
                    """, UUID.class, quotation.quotationId(), versionNumber, access.currencyCode(),
                    request.validityDate(), blankToNull(request.paymentTerms()), blankToNull(request.incoterms()),
                    request.leadTimeDays(), request.minimumOrderQuantity(), blankToNull(request.warrantyTerms()),
                    blankToNull(request.commercialRemarks()), blankToNull(request.technicalDeviations()),
                    calculateTotal(access, request.lines()));
        }

        for (QuotationLineRequest line : request.lines()) {
            insertLine(versionId, access, line);
        }
        jdbc.update("UPDATE srm.quotation SET lifecycle_status = 'DRAFT' WHERE quotation_id = ?",
                quotation.quotationId());
        audit(userId, access.supplierId(), rfqId, quotation.quotationId(),
                latest.isEmpty() || !"DRAFT".equals(latest.getFirst().status())
                        ? "QUOTATION_DRAFT_CREATED" : "QUOTATION_DRAFT_UPDATED", versionNumber);
        return response(access, userId);
    }

    @Transactional
    public SupplierRfqResponse submit(UUID rfqId, UUID userId) {
        RfqAccess access = requireAccess(rfqId, userId);
        ensureBeforeDeadline(access);
        QuotationHeader quotation = jdbc.queryForObject("""
                SELECT quotation_id, lifecycle_status FROM srm.quotation
                WHERE rfq_id = ? AND supplier_id = ? FOR UPDATE
                """, (rs, row) -> new QuotationHeader(
                rs.getObject("quotation_id", UUID.class), rs.getString("lifecycle_status")),
                rfqId, access.supplierId());

        List<VersionHeader> latest = jdbc.query("""
                SELECT quotation_version_id, version_number, version_status
                FROM srm.quotation_version WHERE quotation_id = ?
                ORDER BY version_number DESC LIMIT 1 FOR UPDATE
                """, (rs, row) -> new VersionHeader(
                rs.getObject("quotation_version_id", UUID.class), rs.getInt("version_number"),
                rs.getString("version_status")), quotation.quotationId());
        if (latest.isEmpty() || !"DRAFT".equals(latest.getFirst().status())) {
            throw new ResponseStatusException(BAD_REQUEST, "Save a quotation draft before submitting");
        }

        VersionHeader draft = latest.getFirst();
        Integer lineCount = jdbc.queryForObject("""
                SELECT count(*) FROM srm.quotation_line WHERE quotation_version_id = ?
                """, Integer.class, draft.versionId());
        Integer expectedLines = jdbc.queryForObject("""
                SELECT count(*) FROM srm.rfq_line WHERE rfq_id = ?
                """, Integer.class, rfqId);
        List<VersionTerms> terms = jdbc.query("""
                SELECT validity_date, lead_time_days FROM srm.quotation_version
                WHERE quotation_version_id = ?
                """, (rs, row) -> new VersionTerms(
                rs.getObject("validity_date", LocalDate.class), (Integer) rs.getObject("lead_time_days")),
                draft.versionId());

        if (lineCount == null || expectedLines == null || !Objects.equals(lineCount, expectedLines)) {
            throw new ResponseStatusException(BAD_REQUEST, "Quote every RFQ line before submitting");
        }
        if (terms.isEmpty() || terms.getFirst().validityDate() == null
                || terms.getFirst().validityDate().isBefore(LocalDate.now())
                || terms.getFirst().leadTimeDays() == null || terms.getFirst().leadTimeDays() < 0) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "A future validity date and non-negative delivery lead time are required");
        }

        int updated = jdbc.update("""
                UPDATE srm.quotation_version
                SET version_status = 'SUBMITTED', submitted_by = ?, submitted_at = now()
                WHERE quotation_version_id = ? AND version_status = 'DRAFT'
                """, userId, draft.versionId());
        if (updated != 1) {
            throw new ResponseStatusException(CONFLICT, "Quotation draft changed; refresh and retry");
        }
        jdbc.update("UPDATE srm.quotation SET lifecycle_status = 'SUBMITTED' WHERE quotation_id = ?",
                quotation.quotationId());
        audit(userId, access.supplierId(), rfqId, quotation.quotationId(),
                draft.versionNumber() == 1 ? "QUOTATION_SUBMITTED" : "QUOTATION_REVISED", draft.versionNumber());
        return response(access, userId);
    }

    @Transactional
    public SupplierRfqResponse withdraw(UUID rfqId, UUID userId, QuotationWithdrawalRequest request) {
        RfqAccess access = requireAccess(rfqId, userId);
        ensureBeforeDeadline(access);
        QuotationHeader quotation = jdbc.queryForObject("""
                SELECT quotation_id, lifecycle_status FROM srm.quotation
                WHERE rfq_id = ? AND supplier_id = ? FOR UPDATE
                """, (rs, row) -> new QuotationHeader(
                rs.getObject("quotation_id", UUID.class), rs.getString("lifecycle_status")),
                rfqId, access.supplierId());

        List<VersionHeader> latest = jdbc.query("""
                SELECT quotation_version_id, version_number, version_status
                FROM srm.quotation_version WHERE quotation_id = ?
                ORDER BY version_number DESC LIMIT 1 FOR UPDATE
                """, (rs, row) -> new VersionHeader(
                rs.getObject("quotation_version_id", UUID.class), rs.getInt("version_number"),
                rs.getString("version_status")), quotation.quotationId());
        if (latest.isEmpty() || !"SUBMITTED".equals(latest.getFirst().status())) {
            throw new ResponseStatusException(CONFLICT, "Only the current submitted quotation can be withdrawn");
        }

        VersionHeader submitted = latest.getFirst();
        int updated = jdbc.update("""
                UPDATE srm.quotation_version SET version_status = 'WITHDRAWN'
                WHERE quotation_version_id = ? AND version_status = 'SUBMITTED'
                """, submitted.versionId());
        if (updated != 1) {
            throw new ResponseStatusException(CONFLICT, "Quotation changed; refresh and retry");
        }
        jdbc.update("UPDATE srm.quotation SET lifecycle_status = 'WITHDRAWN' WHERE quotation_id = ?",
                quotation.quotationId());
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id,
                     rfq_id, quotation_id, outcome, changed_fields)
                VALUES (?, 'SUPPLIER_USER', 'QUOTATION_WITHDRAWN', 'QUOTATION', ?, ?, ?, ?, 'SUCCESS',
                        jsonb_build_object('version_number', ?, 'reason', ?))
                """, userId, quotation.quotationId(), access.supplierId(), rfqId, quotation.quotationId(),
                submitted.versionNumber(), request.reason().trim());
        jdbc.update("""
                INSERT INTO srm.notification
                    (recipient_user_id, event_type, channel, delivery_status, title, body, target_path)
                SELECT r.buyer_user_id, 'QUOTATION_WITHDRAWN', channels.channel, 'QUEUED',
                       'Quotation withdrawn for RFQ ' || r.rfq_number,
                       'An invited supplier withdrew its submitted quotation. Review the RFQ quotation status.',
                       '/evaluations'
                FROM srm.rfq r
                CROSS JOIN (VALUES ('IN_APP'), ('EMAIL')) AS channels(channel)
                WHERE r.rfq_id = ?
                """, rfqId);
        return response(access, userId);
    }

    private RfqAccess requireAccess(UUID rfqId, UUID userId) {
        List<RfqAccess> rows = jdbc.query("""
                SELECT r.rfq_id, r.rfq_number, r.title, r.description, r.currency_code,
                       r.submission_deadline, r.required_delivery_date, r.payment_terms, r.incoterms,
                       s.supplier_id, i.invitation_status
                FROM srm.rfq r
                JOIN srm.rfq_invitation i ON i.rfq_id = r.rfq_id
                JOIN srm.supplier_organization s ON s.supplier_id = i.supplier_id
                JOIN srm.supplier_user_membership m ON m.supplier_id = s.supplier_id
                WHERE r.rfq_id = ? AND m.user_id = ? AND m.membership_status = 'ACTIVE'
                  AND s.lifecycle_status IN ('APPROVED', 'ACTIVE')
                  AND i.invitation_status IN ('INVITED', 'ACKNOWLEDGED')
                  AND r.status = 'PUBLISHED'
                """, (rs, row) -> new RfqAccess(
                rs.getObject("rfq_id", UUID.class), rs.getString("rfq_number"), rs.getString("title"),
                rs.getString("description"), rs.getString("currency_code"),
                rs.getTimestamp("submission_deadline").toInstant(),
                rs.getObject("required_delivery_date", LocalDate.class),
                rs.getString("payment_terms"), rs.getString("incoterms"),
                rs.getObject("supplier_id", UUID.class), rs.getString("invitation_status")
        ), rfqId, userId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Invited RFQ not found");
        }
        return rows.getFirst();
    }

    private void validateCurrencyAndLines(RfqAccess access, QuotationDraftRequest request) {
        if (!access.currencyCode().equals(request.currencyCode())) {
            throw new ResponseStatusException(BAD_REQUEST, "Quotation currency must match the RFQ currency");
        }
        List<RfqLineQuantity> rfqLines = jdbc.query("""
                SELECT rfq_line_id, quantity FROM srm.rfq_line WHERE rfq_id = ?
                """, (rs, row) -> new RfqLineQuantity(
                rs.getObject("rfq_line_id", UUID.class), rs.getBigDecimal("quantity")), access.rfqId());
        Map<UUID, BigDecimal> quantities = new HashMap<>();
        rfqLines.forEach(line -> quantities.put(line.rfqLineId(), line.quantity()));
        Set<UUID> seen = new HashSet<>();
        for (QuotationLineRequest line : request.lines()) {
            if (!quantities.containsKey(line.rfqLineId()) || !seen.add(line.rfqLineId())) {
                throw new ResponseStatusException(BAD_REQUEST, "Quotation lines must match RFQ lines exactly");
            }
            BigDecimal discount = valueOrZero(line.discountAmount());
            if (discount.compareTo(line.basicUnitPrice()) > 0) {
                throw new ResponseStatusException(BAD_REQUEST, "Discount cannot exceed the basic unit price");
            }
        }
        if (request.leadTimeDays() != null && request.leadTimeDays() < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Delivery lead time cannot be negative");
        }
        if (request.minimumOrderQuantity() != null && request.minimumOrderQuantity().signum() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Minimum order quantity must be positive");
        }
    }

    private BigDecimal calculateTotal(RfqAccess access, List<QuotationLineRequest> lines) {
        Map<UUID, BigDecimal> quantities = new HashMap<>();
        jdbc.query("""
                SELECT rfq_line_id, quantity FROM srm.rfq_line WHERE rfq_id = ?
                """, (rs, row) -> {
            quantities.put(rs.getObject("rfq_line_id", UUID.class), rs.getBigDecimal("quantity"));
            return null;
        }, access.rfqId());
        return lines.stream().map(line -> landedTotal(line, quantities.get(line.rfqLineId())))
                .reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal landedTotal(QuotationLineRequest line, BigDecimal quantity) {
        BigDecimal netUnit = line.basicUnitPrice().subtract(valueOrZero(line.discountAmount()));
        BigDecimal extended = netUnit.multiply(quantity);
        BigDecimal charges = valueOrZero(line.taxAmount())
                .add(valueOrZero(line.freightAmount()))
                .add(valueOrZero(line.packingAmount()))
                .add(valueOrZero(line.toolingAmount()))
                .add(valueOrZero(line.insuranceAmount()))
                .add(valueOrZero(line.otherCharges()));
        return extended.add(charges).setScale(2, RoundingMode.HALF_UP);
    }

    private void insertLine(UUID versionId, RfqAccess access, QuotationLineRequest line) {
        BigDecimal discount = valueOrZero(line.discountAmount());
        BigDecimal netUnit = line.basicUnitPrice().subtract(discount).setScale(4, RoundingMode.HALF_UP);
        BigDecimal quantity = jdbc.queryForObject("""
                SELECT quantity FROM srm.rfq_line WHERE rfq_id = ? AND rfq_line_id = ?
                """, BigDecimal.class, access.rfqId(), line.rfqLineId());
        BigDecimal landedTotal = landedTotal(line, quantity);
        jdbc.update("""
                INSERT INTO srm.quotation_line
                    (quotation_version_id, rfq_line_id, basic_unit_price, discount_amount, net_unit_price,
                     tax_amount, freight_amount, packing_amount, tooling_amount, insurance_amount,
                     other_charges, landed_line_total)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, versionId, line.rfqLineId(), line.basicUnitPrice(), discount, netUnit,
                valueOrZero(line.taxAmount()), valueOrZero(line.freightAmount()),
                valueOrZero(line.packingAmount()), valueOrZero(line.toolingAmount()),
                valueOrZero(line.insuranceAmount()), valueOrZero(line.otherCharges()), landedTotal);
    }

    private SupplierRfqResponse response(RfqAccess access, UUID userId) {
        List<SupplierRfqLine> lines = jdbc.query("""
                SELECT rfq_line_id, line_number, sap_material_code, description,
                       quantity, uom_code, plant_code, technical_specifications
                FROM srm.rfq_line WHERE rfq_id = ? ORDER BY line_number
                """, (rs, row) -> new SupplierRfqLine(
                rs.getObject("rfq_line_id", UUID.class), rs.getInt("line_number"),
                rs.getString("sap_material_code"), rs.getString("description"),
                rs.getBigDecimal("quantity"), rs.getString("uom_code"), rs.getString("plant_code"),
                rs.getString("technical_specifications")
        ), access.rfqId());
        List<UUID> quotationIds = jdbc.query("""
                SELECT quotation_id FROM srm.quotation WHERE rfq_id = ? AND supplier_id = ?
                """, (rs, row) -> rs.getObject("quotation_id", UUID.class), access.rfqId(), access.supplierId());
        List<QuotationVersionResponse> versions = quotationIds.isEmpty()
                ? List.of() : findVersions(quotationIds.getFirst());
        return new SupplierRfqResponse(access.rfqId(), access.rfqNumber(), access.title(), access.description(),
                access.currencyCode(), access.deadline(), access.requiredDeliveryDate(), access.paymentTerms(),
                access.incoterms(), access.invitationStatus(), lines, versions);
    }

    private List<QuotationVersionResponse> findVersions(UUID quotationId) {
        return jdbc.query("""
                SELECT quotation_version_id, version_number, version_status, currency_code, validity_date,
                       payment_terms, incoterms, lead_time_days, minimum_order_quantity, warranty_terms,
                       commercial_remarks, technical_deviations, total_landed_cost, submitted_at
                FROM srm.quotation_version WHERE quotation_id = ? ORDER BY version_number DESC
                """, (rs, row) -> mapVersion(rs, quotationId), quotationId);
    }

    private QuotationVersionResponse mapVersion(ResultSet rs, UUID quotationId) throws SQLException {
        UUID versionId = rs.getObject("quotation_version_id", UUID.class);
        List<QuotationLineResponse> lines = jdbc.query("""
                SELECT rfq_line_id, basic_unit_price, discount_amount, net_unit_price, tax_amount,
                       freight_amount, packing_amount, tooling_amount, insurance_amount,
                       other_charges, landed_line_total
                FROM srm.quotation_line WHERE quotation_version_id = ? ORDER BY rfq_line_id
                """, (line, row) -> new QuotationLineResponse(
                line.getObject("rfq_line_id", UUID.class), line.getBigDecimal("basic_unit_price"),
                line.getBigDecimal("discount_amount"), line.getBigDecimal("net_unit_price"),
                line.getBigDecimal("tax_amount"), line.getBigDecimal("freight_amount"),
                line.getBigDecimal("packing_amount"), line.getBigDecimal("tooling_amount"),
                line.getBigDecimal("insurance_amount"), line.getBigDecimal("other_charges"),
                line.getBigDecimal("landed_line_total")
        ), versionId);
        return new QuotationVersionResponse(versionId, rs.getInt("version_number"),
                rs.getString("version_status"), rs.getString("currency_code"),
                rs.getObject("validity_date", LocalDate.class), rs.getString("payment_terms"),
                rs.getString("incoterms"), (Integer) rs.getObject("lead_time_days"),
                rs.getBigDecimal("minimum_order_quantity"), rs.getString("warranty_terms"),
                rs.getString("commercial_remarks"), rs.getString("technical_deviations"),
                rs.getBigDecimal("total_landed_cost"),
                rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toInstant(), lines);
    }

    private void audit(UUID actorId, UUID supplierId, UUID rfqId, UUID quotationId,
                       String action, int versionNumber) {
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id,
                     rfq_id, quotation_id, outcome, changed_fields)
                VALUES (?, 'SUPPLIER_USER', ?, 'QUOTATION', ?, ?, ?, ?, 'SUCCESS',
                        jsonb_build_object('version_number', ?))
                """, actorId, action, quotationId, supplierId, rfqId, quotationId, versionNumber);
    }

    private void ensureBeforeDeadline(RfqAccess access) {
        if (!access.deadline().isAfter(Instant.now())) {
            throw new ResponseStatusException(CONFLICT, "The RFQ submission deadline has passed");
        }
    }

    private static BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record RfqAccess(UUID rfqId, String rfqNumber, String title, String description, String currencyCode,
                             Instant deadline, LocalDate requiredDeliveryDate, String paymentTerms,
                             String incoterms, UUID supplierId, String invitationStatus) {}
    private record QuotationHeader(UUID quotationId, String lifecycleStatus) {}
    private record VersionHeader(UUID versionId, int versionNumber, String status) {}
    private record RfqLineQuantity(UUID rfqLineId, BigDecimal quantity) {}
    private record VersionTerms(LocalDate validityDate, Integer leadTimeDays) {}
}
