package com.srm.erp.evaluation.service;

import com.srm.erp.evaluation.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class QuotationEvaluationService {
    private static final Set<String> TECHNICAL_STATUSES = Set.of(
            "TECHNICALLY_ACCEPTED", "TECHNICALLY_REJECTED",
            "CLARIFICATION_REQUIRED", "CONDITIONALLY_ACCEPTED");
    private final JdbcTemplate jdbc;

    public QuotationEvaluationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public RfqEvaluationHistory history(UUID rfqId, UUID buyerId) {
        RfqHeader rfq = requireBuyerOwnsRfq(rfqId, buyerId);
        List<TechnicalEvaluationRecord> technical = jdbc.query("""
                SELECT s.supplier_id, s.supplier_number, s.legal_name, v.quotation_version_id,
                       v.version_number, t.evaluator_user_id, u.full_name AS evaluator_name,
                       t.technical_status, t.remarks, t.evaluated_at
                FROM srm.technical_evaluation t
                JOIN srm.quotation_version v ON v.quotation_version_id = t.quotation_version_id
                JOIN srm.quotation q ON q.quotation_id = v.quotation_id
                JOIN srm.supplier_organization s ON s.supplier_id = q.supplier_id
                JOIN srm.app_user u ON u.user_id = t.evaluator_user_id
                WHERE q.rfq_id = ? ORDER BY t.evaluated_at DESC
                """, (rs, row) -> mapTechnical(rs), rfqId);
        List<CommercialEvaluationRecord> commercial = jdbc.query("""
                SELECT s.supplier_id, s.supplier_number, s.legal_name, v.quotation_version_id,
                       v.version_number, c.evaluator_user_id, u.full_name AS evaluator_name,
                       c.comparison_basis, c.remarks, c.evaluated_at
                FROM srm.commercial_evaluation c
                JOIN srm.quotation_version v ON v.quotation_version_id = c.quotation_version_id
                JOIN srm.quotation q ON q.quotation_id = v.quotation_id
                JOIN srm.supplier_organization s ON s.supplier_id = q.supplier_id
                JOIN srm.app_user u ON u.user_id = c.evaluator_user_id
                WHERE q.rfq_id = ? ORDER BY c.evaluated_at DESC
                """, (rs, row) -> mapCommercial(rs), rfqId);
        List<SupplierRecommendationRecord> recommendations = jdbc.query("""
                SELECT sr.recommendation_id, s.supplier_id, s.supplier_number, s.legal_name,
                       v.quotation_version_id, v.version_number, sr.recommended_by,
                       u.full_name AS recommender_name, sr.is_non_l1, sr.justification, sr.created_at
                FROM srm.supplier_recommendation sr
                JOIN srm.supplier_organization s ON s.supplier_id = sr.supplier_id
                JOIN srm.quotation_version v ON v.quotation_version_id = sr.quotation_version_id
                JOIN srm.app_user u ON u.user_id = sr.recommended_by
                WHERE sr.rfq_id = ? ORDER BY sr.created_at DESC
                """, (rs, row) -> new SupplierRecommendationRecord(
                rs.getObject("recommendation_id", UUID.class), rs.getObject("supplier_id", UUID.class),
                rs.getString("supplier_number"), rs.getString("legal_name"),
                rs.getObject("quotation_version_id", UUID.class), rs.getInt("version_number"),
                rs.getObject("recommended_by", UUID.class), rs.getString("recommender_name"),
                rs.getBoolean("is_non_l1"), rs.getString("justification"),
                rs.getTimestamp("created_at").toInstant()
        ), rfqId);
        return new RfqEvaluationHistory(rfq.rfqId(), rfq.rfqNumber(), rfq.title(), rfq.currencyCode(),
                technical, commercial, recommendations);
    }

    @Transactional
    public RfqEvaluationHistory evaluate(UUID rfqId, UUID buyerId, EvaluationCreateRequest request) {
        requireBuyerOwnsRfq(rfqId, buyerId);
        if (request == null || request.supplierId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Select a participating supplier");
        }
        String technicalStatus = request.technicalStatus() == null
                ? "" : request.technicalStatus().trim().toUpperCase(Locale.ROOT);
        if (!TECHNICAL_STATUSES.contains(technicalStatus)) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose a supported technical evaluation result");
        }
        if (request.recommendSupplier()
                && (request.recommendationJustification() == null
                || request.recommendationJustification().isBlank())) {
            throw new ResponseStatusException(BAD_REQUEST, "Add a justification for the supplier recommendation");
        }
        List<SubmittedVersion> versions = jdbc.query("""
                SELECT s.supplier_id, s.supplier_number, s.legal_name, v.quotation_version_id,
                       v.version_number, v.total_landed_cost
                FROM srm.quotation q
                JOIN srm.supplier_organization s ON s.supplier_id = q.supplier_id
                JOIN srm.quotation_version v ON v.quotation_id = q.quotation_id
                WHERE q.rfq_id = ? AND q.supplier_id = ? AND v.version_status = 'SUBMITTED'
                ORDER BY v.version_number DESC LIMIT 1
                """, (rs, row) -> new SubmittedVersion(
                rs.getObject("supplier_id", UUID.class), rs.getString("supplier_number"),
                rs.getString("legal_name"), rs.getObject("quotation_version_id", UUID.class),
                rs.getInt("version_number"), rs.getBigDecimal("total_landed_cost")
        ), rfqId, request.supplierId());
        if (versions.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Submitted quotation not found for this RFQ");
        }
        SubmittedVersion version = versions.getFirst();
        jdbc.update("""
                INSERT INTO srm.technical_evaluation
                    (quotation_version_id, evaluator_user_id, technical_status, remarks)
                VALUES (?, ?, ?, ?)
                """, version.versionId(), buyerId, technicalStatus, blankToNull(request.technicalRemarks()));
        jdbc.update("""
                INSERT INTO srm.commercial_evaluation
                    (quotation_version_id, evaluator_user_id, comparison_basis, rank_number, score, remarks)
                VALUES (?, ?, 'MANUAL_REVIEW', NULL, NULL, ?)
                """, version.versionId(), buyerId, request.commercialRemarks().trim());

        if (request.recommendSupplier()) {
            if (version.totalLandedCost() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Quotation total is unavailable for recommendation");
            }
            BigDecimal currentL1 = jdbc.queryForObject("""
                    SELECT min(latest.total_landed_cost)
                    FROM srm.rfq_invitation i
                    JOIN LATERAL (
                        SELECT v.total_landed_cost
                        FROM srm.quotation q
                        JOIN srm.quotation_version v ON v.quotation_id = q.quotation_id
                        WHERE q.rfq_id = i.rfq_id AND q.supplier_id = i.supplier_id
                          AND v.version_status = 'SUBMITTED'
                        ORDER BY v.version_number DESC LIMIT 1
                    ) latest ON true
                    WHERE i.rfq_id = ?
                    """, BigDecimal.class, rfqId);
            boolean nonL1 = currentL1 != null && version.totalLandedCost().compareTo(currentL1) > 0;
            jdbc.update("""
                    INSERT INTO srm.supplier_recommendation
                        (rfq_id, supplier_id, quotation_version_id, recommended_by, is_non_l1, justification)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, rfqId, version.supplierId(), version.versionId(), buyerId, nonL1,
                    request.recommendationJustification().trim());
            audit(buyerId, version.supplierId(), rfqId, version.versionId(),
                    "SUPPLIER_RECOMMENDED", nonL1);
        }
        audit(buyerId, version.supplierId(), rfqId, version.versionId(), "QUOTATION_EVALUATED", false);
        return history(rfqId, buyerId);
    }

    private RfqHeader requireBuyerOwnsRfq(UUID rfqId, UUID buyerId) {
        List<RfqHeader> rows = jdbc.query("""
                SELECT rfq_id, rfq_number, title, currency_code
                FROM srm.rfq WHERE rfq_id = ? AND buyer_user_id = ?
                """, (rs, row) -> new RfqHeader(rs.getObject("rfq_id", UUID.class),
                rs.getString("rfq_number"), rs.getString("title"), rs.getString("currency_code")),
                rfqId, buyerId);
        if (rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "RFQ not found");
        return rows.getFirst();
    }

    private TechnicalEvaluationRecord mapTechnical(ResultSet rs) throws SQLException {
        return new TechnicalEvaluationRecord(rs.getObject("supplier_id", UUID.class),
                rs.getString("supplier_number"), rs.getString("legal_name"),
                rs.getObject("quotation_version_id", UUID.class), rs.getInt("version_number"),
                rs.getObject("evaluator_user_id", UUID.class), rs.getString("evaluator_name"),
                rs.getString("technical_status"), rs.getString("remarks"),
                rs.getTimestamp("evaluated_at").toInstant());
    }

    private CommercialEvaluationRecord mapCommercial(ResultSet rs) throws SQLException {
        return new CommercialEvaluationRecord(rs.getObject("supplier_id", UUID.class),
                rs.getString("supplier_number"), rs.getString("legal_name"),
                rs.getObject("quotation_version_id", UUID.class), rs.getInt("version_number"),
                rs.getObject("evaluator_user_id", UUID.class), rs.getString("evaluator_name"),
                rs.getString("comparison_basis"), rs.getString("remarks"),
                rs.getTimestamp("evaluated_at").toInstant());
    }

    private void audit(UUID actor, UUID supplierId, UUID rfqId, UUID versionId,
                       String action, boolean nonL1) {
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id, rfq_id,
                     outcome, changed_fields)
                VALUES (?, 'PROCUREMENT_BUYER', ?, 'QUOTATION_VERSION', ?, ?, ?, 'SUCCESS',
                        jsonb_build_object('recommended_non_l1', ?, 'actor_user_id', ?))
                """, actor, action, versionId, supplierId, rfqId, nonL1, actor);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record RfqHeader(UUID rfqId, String rfqNumber, String title, String currencyCode) {}
    private record SubmittedVersion(UUID supplierId, String supplierNumber, String supplierName,
                                    UUID versionId, int versionNumber, BigDecimal totalLandedCost) {}
}
