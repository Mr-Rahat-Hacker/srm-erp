package com.srm.erp.approval.service;

import com.srm.erp.approval.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AwardApprovalService {
    private static final Set<String> DECISIONS = Set.of("APPROVE", "REJECT", "RETURN_FOR_REVISION");
    private final JdbcTemplate jdbc;

    public AwardApprovalService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EligibleApproverRecord> eligibleApprovers(UUID buyerId) {
        return jdbc.query("""
                SELECT DISTINCT u.user_id, u.full_name, u.email
                FROM srm.app_user u
                JOIN srm.user_role ur ON ur.user_id = u.user_id
                JOIN srm.role r ON r.role_id = ur.role_id
                WHERE u.account_status = 'ACTIVE' AND r.role_code = 'PROCUREMENT_APPROVER'
                  AND u.user_id <> ?
                ORDER BY u.full_name
                """, (rs, row) -> new EligibleApproverRecord(
                rs.getObject("user_id", UUID.class), rs.getString("full_name"), rs.getString("email")),
                buyerId);
    }

    public List<ApprovalTaskRecord> rfqRequests(UUID rfqId, UUID buyerId) {
        requireBuyerOwnsRfq(rfqId, buyerId);
        return jdbc.query("""
                SELECT ar.approval_request_id, at.approval_task_id, r.rfq_id, r.rfq_number, r.title,
                       sr.recommendation_id, s.supplier_id, s.supplier_number, s.legal_name,
                       v.quotation_version_id, v.version_number, sr.justification,
                       ar.requested_by, requester.full_name AS requester_name, ar.status,
                       ar.created_at, at.due_at
                FROM srm.approval_request ar
                JOIN srm.approval_task at ON at.approval_request_id = ar.approval_request_id
                JOIN srm.supplier_recommendation sr ON sr.recommendation_id = ar.entity_id
                JOIN srm.rfq r ON r.rfq_id = sr.rfq_id
                JOIN srm.supplier_organization s ON s.supplier_id = sr.supplier_id
                JOIN srm.quotation_version v ON v.quotation_version_id = sr.quotation_version_id
                JOIN srm.app_user requester ON requester.user_id = ar.requested_by
                WHERE ar.entity_type = 'AWARD' AND r.rfq_id = ?
                ORDER BY ar.created_at DESC
                """, (rs, row) -> mapTask(rs), rfqId);
    }

    @Transactional
    public ApprovalTaskRecord requestApproval(UUID rfqId, UUID buyerId, AwardApprovalRequest request) {
        if (request == null || request.recommendationId() == null || request.approverUserId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Select a recommendation and an approver");
        }
        requireBuyerOwnsRfq(rfqId, buyerId);
        Recommendation recommendation = jdbc.query("""
                SELECT sr.recommendation_id, sr.recommended_by, sr.justification, sr.supplier_id,
                       s.supplier_number, s.legal_name, sr.quotation_version_id, v.version_number
                FROM srm.supplier_recommendation sr
                JOIN srm.supplier_organization s ON s.supplier_id = sr.supplier_id
                JOIN srm.quotation_version v ON v.quotation_version_id = sr.quotation_version_id
                WHERE sr.recommendation_id = ? AND sr.rfq_id = ?
                FOR UPDATE OF sr
                """, rs -> rs.next() ? mapRecommendation(rs) : null, request.recommendationId(), rfqId);
        if (recommendation == null) throw new ResponseStatusException(NOT_FOUND, "Recommendation not found");
        Boolean pending = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM srm.approval_request
                    WHERE entity_type = 'AWARD' AND entity_id = ? AND status = 'PENDING')
                """, Boolean.class, recommendation.id());
        if (Boolean.TRUE.equals(pending)) {
            throw new ResponseStatusException(CONFLICT, "This recommendation already has a pending approval request");
        }
        if (recommendation.recommenderId().equals(request.approverUserId())) {
            throw new ResponseStatusException(BAD_REQUEST, "The recommender cannot approve the same award");
        }
        Boolean eligible = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM srm.app_user u
                    JOIN srm.user_role ur ON ur.user_id = u.user_id
                    JOIN srm.role r ON r.role_id = ur.role_id
                    WHERE u.user_id = ? AND u.account_status = 'ACTIVE'
                      AND r.role_code = 'PROCUREMENT_APPROVER')
                """, Boolean.class, request.approverUserId());
        if (!Boolean.TRUE.equals(eligible)) {
            throw new ResponseStatusException(BAD_REQUEST, "Assigned user is not an active procurement approver");
        }
        UUID approvalRequestId = jdbc.queryForObject("""
                INSERT INTO srm.approval_request
                    (entity_type, entity_id, workflow_code, status, requested_by)
                VALUES ('AWARD', ?, 'AWARD_SINGLE_STAGE', 'PENDING', ?)
                RETURNING approval_request_id
                """, UUID.class, recommendation.id(), buyerId);
        UUID taskId = jdbc.queryForObject("""
                INSERT INTO srm.approval_task
                    (approval_request_id, stage_code, sequence_number, assigned_user_id, task_status)
                VALUES (?, 'AWARD', 1, ?, 'PENDING')
                RETURNING approval_task_id
                """, UUID.class, approvalRequestId, request.approverUserId());
        audit(buyerId, "AWARD_APPROVAL_REQUESTED", "APPROVAL_REQUEST", approvalRequestId,
                rfqId, "SUCCESS", recommendation.id().toString());
        return new ApprovalTaskRecord(approvalRequestId, taskId, rfqId, null, null,
                recommendation.id(), recommendation.supplierId(), recommendation.supplierNumber(),
                recommendation.supplierName(), recommendation.versionId(), recommendation.versionNumber(),
                recommendation.justification(), buyerId, null, "PENDING", null, null);
    }

    public List<ApprovalTaskRecord> myTasks(UUID approverId) {
        return jdbc.query("""
                SELECT ar.approval_request_id, at.approval_task_id, r.rfq_id, r.rfq_number, r.title,
                       sr.recommendation_id, s.supplier_id, s.supplier_number, s.legal_name,
                       v.quotation_version_id, v.version_number, sr.justification,
                       ar.requested_by, requester.full_name AS requester_name, ar.status,
                       ar.created_at, at.due_at
                FROM srm.approval_task at
                JOIN srm.approval_request ar ON ar.approval_request_id = at.approval_request_id
                JOIN srm.supplier_recommendation sr ON sr.recommendation_id = ar.entity_id
                JOIN srm.rfq r ON r.rfq_id = sr.rfq_id
                JOIN srm.supplier_organization s ON s.supplier_id = sr.supplier_id
                JOIN srm.quotation_version v ON v.quotation_version_id = sr.quotation_version_id
                JOIN srm.app_user requester ON requester.user_id = ar.requested_by
                WHERE ar.entity_type = 'AWARD' AND at.assigned_user_id = ?
                ORDER BY ar.created_at DESC
                """, (rs, row) -> mapTask(rs), approverId);
    }

    @Transactional
    public ApprovalTaskRecord decide(UUID taskId, UUID approverId, ApprovalDecisionRequest request) {
        if (request == null || request.decision() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose an approval decision");
        }
        String decision = request.decision().trim().toUpperCase(Locale.ROOT);
        if (!DECISIONS.contains(decision)) {
            throw new ResponseStatusException(BAD_REQUEST, "Decision must be APPROVE, REJECT or RETURN_FOR_REVISION");
        }
        if (request.comments() == null || request.comments().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Add comments before recording an approval decision");
        }
        ApprovalTaskRecord task = jdbc.query("""
                SELECT ar.approval_request_id, at.approval_task_id, r.rfq_id, r.rfq_number, r.title,
                       sr.recommendation_id, s.supplier_id, s.supplier_number, s.legal_name,
                       v.quotation_version_id, v.version_number, sr.justification,
                       ar.requested_by, requester.full_name AS requester_name, ar.status,
                       ar.created_at, at.due_at
                FROM srm.approval_task at
                JOIN srm.approval_request ar ON ar.approval_request_id = at.approval_request_id
                JOIN srm.supplier_recommendation sr ON sr.recommendation_id = ar.entity_id
                JOIN srm.rfq r ON r.rfq_id = sr.rfq_id
                JOIN srm.supplier_organization s ON s.supplier_id = sr.supplier_id
                JOIN srm.quotation_version v ON v.quotation_version_id = sr.quotation_version_id
                JOIN srm.app_user requester ON requester.user_id = ar.requested_by
                WHERE at.approval_task_id = ? AND at.assigned_user_id = ?
                  AND ar.entity_type = 'AWARD'
                FOR UPDATE OF ar, at
                """, rs -> rs.next() ? mapTask(rs) : null, taskId, approverId);
        if (task == null) throw new ResponseStatusException(NOT_FOUND, "Assigned approval task not found");
        if (!"PENDING".equals(task.status())) {
            throw new ResponseStatusException(CONFLICT, "Approval task has already been decided");
        }
        if (task.requestedBy().equals(approverId)) {
            throw new ResponseStatusException(FORBIDDEN, "Requester cannot approve their own award request");
        }
        String status = switch (decision) {
            case "APPROVE" -> "APPROVED";
            case "REJECT" -> "REJECTED";
            default -> "RETURNED";
        };
        jdbc.update("UPDATE srm.approval_task SET task_status = ? WHERE approval_task_id = ? AND task_status = 'PENDING'",
                status, taskId);
        jdbc.update("""
                INSERT INTO srm.approval_decision (approval_task_id, actor_user_id, decision, remarks)
                VALUES (?, ?, ?, ?)
                """, taskId, approverId, decision, blankToNull(request.comments()));
        jdbc.update("UPDATE srm.approval_request SET status = ? WHERE approval_request_id = ?",
                status, task.approvalRequestId());
        audit(approverId, "AWARD_" + decision, "APPROVAL_REQUEST", task.approvalRequestId(),
                task.rfqId(), "SUCCESS", blankToNull(request.comments()));
        return new ApprovalTaskRecord(task.approvalRequestId(), task.approvalTaskId(), task.rfqId(),
                task.rfqNumber(), task.rfqTitle(), task.recommendationId(), task.supplierId(),
                task.supplierNumber(), task.supplierName(), task.quotationVersionId(), task.versionNumber(),
                task.justification(), task.requestedBy(), task.requesterName(), status, task.createdAt(), task.dueAt());
    }

    @Transactional
    public AwardResponse award(UUID rfqId, UUID buyerId, UUID approvalRequestId) {
        requireBuyerOwnsRfq(rfqId, buyerId);
        ApprovalTaskRecord task = jdbc.query("""
                SELECT ar.approval_request_id, at.approval_task_id, r.rfq_id, r.rfq_number, r.title,
                       sr.recommendation_id, s.supplier_id, s.supplier_number, s.legal_name,
                       v.quotation_version_id, v.version_number, sr.justification,
                       ar.requested_by, requester.full_name AS requester_name, ar.status,
                       ar.created_at, at.due_at
                FROM srm.approval_request ar
                JOIN srm.approval_task at ON at.approval_request_id = ar.approval_request_id
                JOIN srm.supplier_recommendation sr ON sr.recommendation_id = ar.entity_id
                JOIN srm.rfq r ON r.rfq_id = sr.rfq_id
                JOIN srm.supplier_organization s ON s.supplier_id = sr.supplier_id
                JOIN srm.quotation_version v ON v.quotation_version_id = sr.quotation_version_id
                JOIN srm.app_user requester ON requester.user_id = ar.requested_by
                WHERE ar.approval_request_id = ? AND r.rfq_id = ? AND ar.entity_type = 'AWARD'
                FOR UPDATE OF ar, r
                """, rs -> rs.next() ? mapTask(rs) : null, approvalRequestId, rfqId);
        if (task == null) throw new ResponseStatusException(NOT_FOUND, "Approved award request not found");
        if (!"APPROVED".equals(task.status())) {
            throw new ResponseStatusException(CONFLICT, "Award requires an approved request");
        }
        Boolean exists = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM srm.award WHERE rfq_id = ?)", Boolean.class, rfqId);
        if (Boolean.TRUE.equals(exists)) throw new ResponseStatusException(CONFLICT, "RFQ already has a final award");
        AwardResponse award = jdbc.queryForObject("""
                INSERT INTO srm.award
                    (rfq_id, supplier_id, quotation_version_id, approval_request_id,
                     approved_value, currency_code, award_status, awarded_at)
                SELECT r.rfq_id, sr.supplier_id, sr.quotation_version_id, ar.approval_request_id,
                       v.total_landed_cost, r.currency_code, 'APPROVED', now()
                FROM srm.approval_request ar
                JOIN srm.supplier_recommendation sr ON sr.recommendation_id = ar.entity_id
                JOIN srm.rfq r ON r.rfq_id = sr.rfq_id
                JOIN srm.quotation_version v ON v.quotation_version_id = sr.quotation_version_id
                WHERE ar.approval_request_id = ?
                RETURNING award_id, rfq_id, supplier_id, quotation_version_id, approval_request_id,
                          approved_value, currency_code, award_status, awarded_at
                """, (rs, row) -> new AwardResponse(rs.getObject("award_id", UUID.class),
                rs.getObject("rfq_id", UUID.class), task.rfqNumber(), task.supplierId(), task.supplierName(),
                rs.getObject("quotation_version_id", UUID.class), task.versionNumber(),
                rs.getObject("approval_request_id", UUID.class), rs.getBigDecimal("approved_value"),
                rs.getString("currency_code"), rs.getString("award_status"),
                rs.getTimestamp("awarded_at").toInstant()), approvalRequestId);
        jdbc.update("UPDATE srm.rfq SET status = 'AWARDED', updated_at = now() WHERE rfq_id = ?", rfqId);
        jdbc.update("""
                INSERT INTO srm.notification (recipient_user_id, event_type, channel, delivery_status)
                SELECT m.user_id, CASE WHEN m.supplier_id = ? THEN 'AWARD_SELECTED' ELSE 'AWARD_NOT_SELECTED' END,
                       'IN_APP', 'QUEUED'
                FROM srm.rfq_invitation i
                JOIN srm.supplier_user_membership m ON m.supplier_id = i.supplier_id
                WHERE i.rfq_id = ? AND m.membership_status = 'ACTIVE'
                """, task.supplierId(), rfqId);
        audit(buyerId, "RFQ_AWARDED", "AWARD", award.awardId(), rfqId, "SUCCESS", task.supplierId().toString());
        return award;
    }

    private void requireBuyerOwnsRfq(UUID rfqId, UUID buyerId) {
        Boolean owns = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM srm.rfq WHERE rfq_id = ? AND buyer_user_id = ?)",
                Boolean.class, rfqId, buyerId);
        if (!Boolean.TRUE.equals(owns)) throw new ResponseStatusException(NOT_FOUND, "RFQ not found");
    }

    private void audit(UUID actor, String action, String entityType, UUID entityId, UUID rfqId,
                       String outcome, String reason) {
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, rfq_id,
                     outcome, reason, effective_roles)
                VALUES (?, 'USER', ?, ?, ?, ?, ?, ?, ARRAY['PROCUREMENT_USER'])
                """, actor, action, entityType, entityId, rfqId, outcome, reason);
    }

    private ApprovalTaskRecord mapTask(ResultSet rs) throws SQLException {
        return new ApprovalTaskRecord(rs.getObject("approval_request_id", UUID.class),
                rs.getObject("approval_task_id", UUID.class), rs.getObject("rfq_id", UUID.class),
                rs.getString("rfq_number"), rs.getString("title"),
                rs.getObject("recommendation_id", UUID.class), rs.getObject("supplier_id", UUID.class),
                rs.getString("supplier_number"), rs.getString("legal_name"),
                rs.getObject("quotation_version_id", UUID.class), rs.getInt("version_number"),
                rs.getString("justification"), rs.getObject("requested_by", UUID.class),
                rs.getString("requester_name"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("due_at") == null ? null : rs.getTimestamp("due_at").toInstant());
    }

    private Recommendation mapRecommendation(ResultSet rs) throws SQLException {
        return new Recommendation(rs.getObject("recommendation_id", UUID.class),
                rs.getObject("recommended_by", UUID.class), rs.getString("justification"),
                rs.getObject("supplier_id", UUID.class), rs.getString("supplier_number"),
                rs.getString("legal_name"), rs.getObject("quotation_version_id", UUID.class),
                rs.getInt("version_number"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Recommendation(UUID id, UUID recommenderId, String justification, UUID supplierId,
                                  String supplierNumber, String supplierName, UUID versionId, int versionNumber) {}
}
