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
        jdbc.update("""
                INSERT INTO srm.notification
                    (recipient_user_id, event_type, channel, delivery_status, title, body, target_path)
                SELECT ?, 'APPROVAL_TASK_ASSIGNED', channels.channel, 'QUEUED',
                       'Award approval required for RFQ ' || r.rfq_number,
                       'Review the buyer recommendation and record your decision in SRM.',
                       '/approvals'
                FROM srm.rfq r
                CROSS JOIN (VALUES ('IN_APP'), ('EMAIL')) AS channels(channel)
                WHERE r.rfq_id = ?
                """, request.approverUserId(), rfqId);
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

    public ApprovalReviewContext reviewContext(UUID taskId, UUID approverId) {
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
                """, rs -> rs.next() ? mapTask(rs) : null, taskId, approverId);
        if (task == null) throw new ResponseStatusException(NOT_FOUND, "Assigned approval task not found");

        ApprovalReviewContext.RfqContext rfq = jdbc.query("""
                SELECT rfq_id, rfq_number, title, description, currency_code, submission_deadline,
                       required_delivery_date, payment_terms, incoterms, plant_code, purchasing_group
                FROM srm.rfq WHERE rfq_id = ?
                """, rs -> rs.next() ? new ApprovalReviewContext.RfqContext(
                rs.getObject("rfq_id", UUID.class), rs.getString("rfq_number"), rs.getString("title"),
                rs.getString("description"), rs.getString("currency_code"),
                rs.getTimestamp("submission_deadline").toInstant(),
                rs.getDate("required_delivery_date") == null ? null : rs.getDate("required_delivery_date").toLocalDate(),
                rs.getString("payment_terms"), rs.getString("incoterms"), rs.getString("plant_code"),
                rs.getString("purchasing_group")) : null, task.rfqId());
        List<ApprovalReviewContext.QuotationContext> quotations = jdbc.query("""
                SELECT s.supplier_id, s.supplier_number, s.legal_name, v.quotation_version_id,
                       v.version_number, v.submitted_at, v.currency_code, v.total_landed_cost,
                       v.lead_time_days, v.validity_date, v.incoterms, q.technical_deviations
                FROM srm.quotation q
                JOIN srm.supplier_organization s ON s.supplier_id = q.supplier_id
                JOIN LATERAL (
                    SELECT * FROM srm.quotation_version version
                    WHERE version.quotation_id = q.quotation_id AND version.version_status = 'SUBMITTED'
                    ORDER BY version.version_number DESC LIMIT 1
                ) v ON true
                WHERE q.rfq_id = ?
                ORDER BY s.legal_name
                """, (rs, row) -> new ApprovalReviewContext.QuotationContext(
                rs.getObject("supplier_id", UUID.class), rs.getString("supplier_number"),
                rs.getString("legal_name"), rs.getObject("quotation_version_id", UUID.class),
                rs.getInt("version_number"),
                rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toInstant(),
                rs.getString("currency_code"), rs.getBigDecimal("total_landed_cost"),
                rs.getObject("lead_time_days", Integer.class),
                rs.getDate("validity_date") == null ? null : rs.getDate("validity_date").toLocalDate(),
                rs.getString("incoterms"), rs.getString("technical_deviations"),
                quotationLines(rs.getObject("quotation_version_id", UUID.class))), task.rfqId());
        List<ApprovalReviewContext.NegotiationActivity> negotiations = jdbc.query("""
                SELECT n.supplier_id, s.legal_name, n.round_number, n.status AS round_status,
                       n.response_deadline, n.outcome, n.outcome_remarks, u.full_name AS sender_name,
                       m.message_type, m.message_text, m.created_at AS message_created_at,
                       v.version_number AS quotation_version
                FROM srm.negotiation_round n
                JOIN srm.supplier_organization s ON s.supplier_id = n.supplier_id
                LEFT JOIN srm.negotiation_message m ON m.negotiation_round_id = n.negotiation_round_id
                LEFT JOIN srm.app_user u ON u.user_id = m.sender_user_id
                LEFT JOIN srm.quotation_version v ON v.quotation_version_id = m.quotation_version_id
                WHERE n.rfq_id = ?
                ORDER BY n.round_number, m.created_at
                """, (rs, row) -> new ApprovalReviewContext.NegotiationActivity(
                rs.getObject("supplier_id", UUID.class), rs.getString("legal_name"),
                rs.getInt("round_number"), rs.getString("round_status"),
                rs.getTimestamp("response_deadline") == null ? null : rs.getTimestamp("response_deadline").toInstant(),
                rs.getString("outcome"), rs.getString("outcome_remarks"), rs.getString("sender_name"),
                rs.getString("message_type"), rs.getString("message_text"),
                rs.getTimestamp("message_created_at") == null ? null : rs.getTimestamp("message_created_at").toInstant(),
                rs.getObject("quotation_version", Integer.class)), task.rfqId());
        List<ApprovalReviewContext.EvaluationContext> evaluations = jdbc.query("""
                SELECT s.supplier_id, s.legal_name, v.version_number, 'TECHNICAL' AS evaluation_type,
                       t.technical_status AS result, u.full_name AS evaluator_name, t.remarks, t.evaluated_at
                FROM srm.technical_evaluation t
                JOIN srm.quotation_version v ON v.quotation_version_id = t.quotation_version_id
                JOIN srm.quotation q ON q.quotation_id = v.quotation_id
                JOIN srm.supplier_organization s ON s.supplier_id = q.supplier_id
                JOIN srm.app_user u ON u.user_id = t.evaluator_user_id
                WHERE q.rfq_id = ?
                UNION ALL
                SELECT s.supplier_id, s.legal_name, v.version_number, 'COMMERCIAL',
                       c.comparison_basis, u.full_name, c.remarks, c.evaluated_at
                FROM srm.commercial_evaluation c
                JOIN srm.quotation_version v ON v.quotation_version_id = c.quotation_version_id
                JOIN srm.quotation q ON q.quotation_id = v.quotation_id
                JOIN srm.supplier_organization s ON s.supplier_id = q.supplier_id
                JOIN srm.app_user u ON u.user_id = c.evaluator_user_id
                WHERE q.rfq_id = ?
                ORDER BY evaluated_at DESC
                """, (rs, row) -> new ApprovalReviewContext.EvaluationContext(
                rs.getObject("supplier_id", UUID.class), rs.getString("legal_name"),
                rs.getInt("version_number"), rs.getString("evaluation_type"), rs.getString("result"),
                rs.getString("evaluator_name"), rs.getString("remarks"),
                rs.getTimestamp("evaluated_at").toInstant()), task.rfqId(), task.rfqId());
        return new ApprovalReviewContext(rfq, task, quotations, negotiations, evaluations);
    }

    private List<ApprovalReviewContext.QuotationLineContext> quotationLines(UUID versionId) {
        return jdbc.query("""
                SELECT l.line_number, l.sap_material_code, l.description, l.quantity, l.uom_code,
                       ql.basic_unit_price, ql.net_unit_price, ql.tax_amount,
                       ql.freight_amount, ql.landed_line_total
                FROM srm.quotation_line ql
                JOIN srm.rfq_line l ON l.rfq_line_id = ql.rfq_line_id
                WHERE ql.quotation_version_id = ?
                ORDER BY l.line_number
                """, (rs, row) -> new ApprovalReviewContext.QuotationLineContext(
                rs.getInt("line_number"), rs.getString("sap_material_code"), rs.getString("description"),
                rs.getBigDecimal("quantity"), rs.getString("uom_code"), rs.getBigDecimal("basic_unit_price"),
                rs.getBigDecimal("net_unit_price"), rs.getBigDecimal("tax_amount"),
                rs.getBigDecimal("freight_amount"), rs.getBigDecimal("landed_line_total")), versionId);
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
        jdbc.update("""
                INSERT INTO srm.notification
                    (recipient_user_id, event_type, channel, delivery_status, title, body, target_path)
                SELECT ?, ?, channels.channel, 'QUEUED',
                       'Award approval update for RFQ ' || r.rfq_number,
                       ?, '/evaluations'
                FROM srm.rfq r
                CROSS JOIN (VALUES ('IN_APP'), ('EMAIL')) AS channels(channel)
                WHERE r.rfq_id = ?
                """, task.requestedBy(), "AWARD_APPROVAL_" + status, task.supplierName() + " recommendation was "
                + status.toLowerCase().replace('_', ' ') + ". " + request.comments(), task.rfqId());
        audit(approverId, "AWARD_" + decision, "APPROVAL_REQUEST", task.approvalRequestId(),
                task.rfqId(), "SUCCESS", blankToNull(request.comments()));
        return new ApprovalTaskRecord(task.approvalRequestId(), task.approvalTaskId(), task.rfqId(),
                task.rfqNumber(), task.rfqTitle(), task.recommendationId(), task.supplierId(),
                task.supplierNumber(), task.supplierName(), task.quotationVersionId(), task.versionNumber(),
                task.justification(), task.requestedBy(), task.requesterName(), status, task.createdAt(), task.dueAt());
    }

    public AwardResponse awardForRfq(UUID rfqId, UUID buyerId) {
        requireBuyerOwnsRfq(rfqId, buyerId);
        return jdbc.query("""
                SELECT a.award_id, a.rfq_id, r.rfq_number, a.supplier_id, s.legal_name,
                       a.quotation_version_id, v.version_number, a.approval_request_id,
                       a.approved_value, a.currency_code, a.award_status, a.awarded_at
                FROM srm.award a
                JOIN srm.rfq r ON r.rfq_id = a.rfq_id
                JOIN srm.supplier_organization s ON s.supplier_id = a.supplier_id
                JOIN srm.quotation_version v ON v.quotation_version_id = a.quotation_version_id
                WHERE a.rfq_id = ?
                """, rs -> rs.next() ? new AwardResponse(rs.getObject("award_id", UUID.class),
                rs.getObject("rfq_id", UUID.class), rs.getString("rfq_number"),
                rs.getObject("supplier_id", UUID.class), rs.getString("legal_name"),
                rs.getObject("quotation_version_id", UUID.class), rs.getInt("version_number"),
                rs.getObject("approval_request_id", UUID.class), rs.getBigDecimal("approved_value"),
                rs.getString("currency_code"), rs.getString("award_status"),
                rs.getTimestamp("awarded_at").toInstant()) : null, rfqId);
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
                INSERT INTO srm.notification
                    (recipient_user_id, event_type, channel, delivery_status, title, body, target_path)
                SELECT m.user_id,
                       CASE WHEN m.supplier_id = ? THEN 'AWARD_SELECTED' ELSE 'AWARD_NOT_SELECTED' END,
                       channels.channel, 'QUEUED',
                       'Award outcome for ' || r.rfq_number,
                       CASE WHEN m.supplier_id = ?
                            THEN 'Your organization was selected for this RFQ. Sign in to review the outcome.'
                            ELSE 'The RFQ award has been recorded. Sign in to review the outcome.'
                       END,
                       '/supplier-portal/quotations'
                FROM srm.rfq_invitation i
                JOIN srm.supplier_user_membership m ON m.supplier_id = i.supplier_id
                JOIN srm.rfq r ON r.rfq_id = i.rfq_id
                CROSS JOIN (VALUES ('IN_APP'), ('EMAIL')) AS channels(channel)
                WHERE i.rfq_id = ? AND m.membership_status = 'ACTIVE'
                """, task.supplierId(), task.supplierId(), rfqId);
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
