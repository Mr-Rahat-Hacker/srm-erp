package com.srm.erp.approval.service;

import com.srm.erp.approval.api.AwardApprovalPolicyRequest;
import com.srm.erp.approval.api.AwardApprovalPolicyResponse;
import com.srm.erp.approval.api.AwardApprovalRequest;
import com.srm.erp.approval.api.ApprovalTaskRecord;
import com.srm.erp.approval.api.EligibleApproverRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AwardApprovalPolicyService {
    private final JdbcTemplate jdbc;
    private final AwardApprovalService approvals;

    public AwardApprovalPolicyService(JdbcTemplate jdbc, AwardApprovalService approvals) {
        this.jdbc = jdbc;
        this.approvals = approvals;
    }

    public List<AwardApprovalPolicyResponse> findAll() {
        List<PolicyHeader> headers = jdbc.query("""
                SELECT policy_id, policy_code, policy_name, currency_code, minimum_amount,
                       maximum_amount, is_active, updated_at
                FROM srm.award_approval_policy
                ORDER BY currency_code, minimum_amount, policy_code
                """, (rs, row) -> mapHeader(rs));
        return headers.stream().map(this::toResponse).toList();
    }

    public List<EligibleApproverRecord> eligibleApprovers() {
        return jdbc.query("""
                SELECT DISTINCT u.user_id, u.full_name, u.email
                FROM srm.app_user u
                JOIN srm.user_role ur ON ur.user_id = u.user_id
                JOIN srm.role r ON r.role_id = ur.role_id
                WHERE u.account_status = 'ACTIVE' AND r.role_code = 'PROCUREMENT_APPROVER'
                ORDER BY u.full_name
                """, (rs, row) -> new EligibleApproverRecord(
                rs.getObject("user_id", UUID.class), rs.getString("full_name"), rs.getString("email")));
    }

    @Transactional
    public AwardApprovalPolicyResponse create(UUID actorId, AwardApprovalPolicyRequest request) {
        validateRequest(request);
        jdbc.execute("LOCK TABLE srm.award_approval_policy IN SHARE ROW EXCLUSIVE MODE");
        ensureCodeAvailable(request.policyCode().trim().toUpperCase(Locale.ROOT), null);
        ensureNoOverlap(request.currencyCode().trim().toUpperCase(Locale.ROOT),
                request.minimumAmount(), request.maximumAmount(), null);
        List<UUID> approverIds = validateApprovers(request.approverUserIds());
        UUID policyId = jdbc.queryForObject("""
                INSERT INTO srm.award_approval_policy
                    (policy_code, policy_name, currency_code, minimum_amount, maximum_amount,
                     is_active, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, true, ?, ?)
                RETURNING policy_id
                """, UUID.class, request.policyCode().trim().toUpperCase(Locale.ROOT),
                request.policyName().trim(), request.currencyCode().trim().toUpperCase(Locale.ROOT),
                request.minimumAmount(), request.maximumAmount(), actorId, actorId);
        replaceStages(policyId, approverIds);
        audit(actorId, "AWARD_APPROVAL_POLICY_CREATED", policyId, request.reason().trim());
        return getPolicy(policyId);
    }

    @Transactional
    public AwardApprovalPolicyResponse update(UUID policyId, UUID actorId, AwardApprovalPolicyRequest request) {
        validateRequest(request);
        jdbc.execute("LOCK TABLE srm.award_approval_policy IN SHARE ROW EXCLUSIVE MODE");
        if (!exists(policyId)) throw new ResponseStatusException(NOT_FOUND, "Award approval policy not found");
        ensureCodeAvailable(request.policyCode().trim().toUpperCase(Locale.ROOT), policyId);
        ensureNoOverlap(request.currencyCode().trim().toUpperCase(Locale.ROOT),
                request.minimumAmount(), request.maximumAmount(), policyId);
        List<UUID> approverIds = validateApprovers(request.approverUserIds());
        jdbc.update("""
                UPDATE srm.award_approval_policy
                SET policy_code = ?, policy_name = ?, currency_code = ?, minimum_amount = ?,
                    maximum_amount = ?, is_active = true, updated_by = ?, updated_at = now()
                WHERE policy_id = ?
                """, request.policyCode().trim().toUpperCase(Locale.ROOT), request.policyName().trim(),
                request.currencyCode().trim().toUpperCase(Locale.ROOT), request.minimumAmount(),
                request.maximumAmount(), actorId, policyId);
        replaceStages(policyId, approverIds);
        audit(actorId, "AWARD_APPROVAL_POLICY_UPDATED", policyId, request.reason().trim());
        return getPolicy(policyId);
    }

    @Transactional
    public void deactivate(UUID policyId, UUID actorId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "A reason is required to deactivate an approval policy");
        }
        jdbc.execute("LOCK TABLE srm.award_approval_policy IN SHARE ROW EXCLUSIVE MODE");
        int updated = jdbc.update("""
                UPDATE srm.award_approval_policy
                SET is_active = false, updated_by = ?, updated_at = now()
                WHERE policy_id = ? AND is_active = true
                """, actorId, policyId);
        if (updated != 1) throw new ResponseStatusException(NOT_FOUND, "Active award approval policy not found");
        audit(actorId, "AWARD_APPROVAL_POLICY_DEACTIVATED", policyId, reason.trim());
    }

    @Transactional
    public ApprovalTaskRecord requestApproval(UUID rfqId, UUID buyerId, UUID recommendationId) {
        Boolean ownsRfq = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM srm.rfq WHERE rfq_id = ? AND buyer_user_id = ?)
                """, Boolean.class, rfqId, buyerId);
        if (!Boolean.TRUE.equals(ownsRfq)) throw new ResponseStatusException(NOT_FOUND, "RFQ not found");

        List<RecommendationAmount> recommendations = jdbc.query("""
                SELECT sr.recommendation_id, sr.recommended_by, v.total_landed_cost, r.currency_code
                FROM srm.supplier_recommendation sr
                JOIN srm.quotation_version v ON v.quotation_version_id = sr.quotation_version_id
                JOIN srm.rfq r ON r.rfq_id = sr.rfq_id
                WHERE sr.recommendation_id = ? AND sr.rfq_id = ?
                FOR SHARE OF sr, v, r
                """, (rs, row) -> new RecommendationAmount(
                rs.getObject("recommendation_id", UUID.class),
                rs.getObject("recommended_by", UUID.class),
                rs.getBigDecimal("total_landed_cost"),
                rs.getString("currency_code")), recommendationId, rfqId);
        if (recommendations.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Recommendation not found");
        RecommendationAmount recommendation = recommendations.getFirst();
        if (recommendation.amount() == null || recommendation.currencyCode() == null) {
            throw new ResponseStatusException(CONFLICT, "Recommendation has no amount or currency for policy matching");
        }

        List<PolicyRoute> matches = jdbc.query("""
                SELECT policy_id, policy_code, minimum_amount, maximum_amount
                FROM srm.award_approval_policy
                WHERE is_active = true AND currency_code = ? AND minimum_amount <= ?
                  AND (maximum_amount IS NULL OR ? < maximum_amount)
                ORDER BY minimum_amount DESC
                FOR SHARE
                """, (rs, row) -> new PolicyRoute(
                rs.getObject("policy_id", UUID.class), rs.getString("policy_code"),
                rs.getBigDecimal("minimum_amount"), rs.getBigDecimal("maximum_amount")),
                recommendation.currencyCode(), recommendation.amount(), recommendation.amount());
        if (matches.isEmpty()) {
            throw new ResponseStatusException(CONFLICT,
                    "No active approval policy matches " + recommendation.currencyCode() + " "
                            + recommendation.amount() + "; ask an SRM administrator to configure a policy");
        }
        if (matches.size() != 1) {
            throw new ResponseStatusException(CONFLICT,
                    "Multiple active approval policies match this amount; ask an SRM administrator to fix the policy ranges");
        }
        PolicyRoute policy = matches.getFirst();
        List<UUID> approverIds = jdbc.query("""
                SELECT assigned_user_id FROM srm.award_approval_policy_stage
                WHERE policy_id = ? ORDER BY sequence_number
                """, (rs, row) -> rs.getObject("assigned_user_id", UUID.class), policy.policyId());
        if (approverIds.isEmpty() || approverIds.size() > 10 || new HashSet<>(approverIds).size() != approverIds.size()) {
            throw new ResponseStatusException(CONFLICT, "The matched approval policy has an invalid approval route");
        }

        ApprovalTaskRecord created = approvals.requestApproval(
                rfqId, buyerId, new AwardApprovalRequest(recommendationId, null, approverIds));
        audit(buyerId, "AWARD_APPROVAL_POLICY_APPLIED", created.approvalRequestId(),
                "policy=" + policy.policyCode() + "; currency=" + recommendation.currencyCode()
                        + "; amount=" + recommendation.amount().toPlainString()
                        + "; range=[" + policy.minimumAmount().toPlainString() + ","
                        + (policy.maximumAmount() == null ? "unbounded" : policy.maximumAmount().toPlainString())
                        + "); stages=" + String.join(" -> ", approverIds.stream().map(UUID::toString).toList()));
        return created;
    }

    private List<UUID> validateApprovers(List<UUID> rawIds) {
        if (rawIds == null || rawIds.isEmpty() || rawIds.size() > 10
                || rawIds.stream().anyMatch(id -> id == null) || new HashSet<>(rawIds).size() != rawIds.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose 1 to 10 distinct approvers");
        }
        List<UUID> ids = new ArrayList<>(rawIds);
        for (UUID id : ids) {
            Boolean eligible = jdbc.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM srm.app_user u
                        JOIN srm.user_role ur ON ur.user_id = u.user_id
                        JOIN srm.role r ON r.role_id = ur.role_id
                        WHERE u.user_id = ? AND u.account_status = 'ACTIVE'
                          AND r.role_code = 'PROCUREMENT_APPROVER')
                    """, Boolean.class, id);
            if (!Boolean.TRUE.equals(eligible)) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "Every policy stage must be assigned to an active procurement approver");
            }
        }
        return ids;
    }

    private void validateRequest(AwardApprovalPolicyRequest request) {
        if (request == null) throw new ResponseStatusException(BAD_REQUEST, "Policy details are required");
        String code = request.policyCode() == null ? "" : request.policyCode().trim();
        String name = request.policyName() == null ? "" : request.policyName().trim();
        String currency = request.currencyCode() == null ? "" : request.currencyCode().trim();
        if (!code.matches("[A-Za-z0-9_-]{1,50}") || name.isBlank() || name.length() > 160
                || !currency.matches("[A-Za-z]{3}") || request.minimumAmount() == null
                || request.minimumAmount().signum() < 0 || request.minimumAmount().scale() > 2
                || request.maximumAmount() != null
                    && (request.maximumAmount().scale() > 2
                    || request.maximumAmount().compareTo(request.minimumAmount()) <= 0)
                || request.reason() == null || request.reason().isBlank() || request.reason().length() > 500) {
            throw new ResponseStatusException(BAD_REQUEST, "Policy fields or amount range are invalid");
        }
    }

    private void ensureCodeAvailable(String code, UUID exceptId) {
        List<Boolean> duplicates = exceptId == null
                ? jdbc.query("SELECT EXISTS (SELECT 1 FROM srm.award_approval_policy WHERE policy_code = ?)",
                    (rs, row) -> rs.getBoolean(1), code)
                : jdbc.query("SELECT EXISTS (SELECT 1 FROM srm.award_approval_policy WHERE policy_code = ? AND policy_id <> ?)",
                    (rs, row) -> rs.getBoolean(1), code, exceptId);
        if (!duplicates.isEmpty() && duplicates.getFirst()) {
            throw new ResponseStatusException(CONFLICT, "An approval policy already uses this code");
        }
    }

    private void ensureNoOverlap(String currency, BigDecimal minimum, BigDecimal maximum, UUID exceptId) {
        String sql = "SELECT minimum_amount, maximum_amount FROM srm.award_approval_policy "
                + "WHERE is_active = true AND currency_code = ?"
                + (exceptId == null ? "" : " AND policy_id <> ?");
        List<PolicyRange> ranges = exceptId == null
                ? jdbc.query(sql, (rs, row) -> new PolicyRange(
                    rs.getBigDecimal("minimum_amount"), rs.getBigDecimal("maximum_amount")), currency)
                : jdbc.query(sql, (rs, row) -> new PolicyRange(
                    rs.getBigDecimal("minimum_amount"), rs.getBigDecimal("maximum_amount")), currency, exceptId);
        for (PolicyRange existing : ranges) {
            boolean beforeExistingEnd = existing.maximumAmount() == null
                    || minimum.compareTo(existing.maximumAmount()) < 0;
            boolean beforeNewEnd = maximum == null
                    || existing.minimumAmount().compareTo(maximum) < 0;
            if (beforeExistingEnd && beforeNewEnd) {
                throw new ResponseStatusException(CONFLICT,
                        "Active approval policy ranges cannot overlap for the same currency");
            }
        }
    }

    private void replaceStages(UUID policyId, List<UUID> approverIds) {
        jdbc.update("DELETE FROM srm.award_approval_policy_stage WHERE policy_id = ?", policyId);
        for (int i = 0; i < approverIds.size(); i++) {
            jdbc.update("""
                    INSERT INTO srm.award_approval_policy_stage (policy_id, sequence_number, assigned_user_id)
                    VALUES (?, ?, ?)
                    """, policyId, i + 1, approverIds.get(i));
        }
    }

    private boolean exists(UUID policyId) {
        Boolean result = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM srm.award_approval_policy WHERE policy_id = ?)
                """, Boolean.class, policyId);
        return Boolean.TRUE.equals(result);
    }

    private AwardApprovalPolicyResponse getPolicy(UUID policyId) {
        PolicyHeader header = jdbc.query("""
                SELECT policy_id, policy_code, policy_name, currency_code, minimum_amount,
                       maximum_amount, is_active, updated_at
                FROM srm.award_approval_policy WHERE policy_id = ?
                """, rs -> rs.next() ? mapHeader(rs) : null, policyId);
        return header == null ? null : toResponse(header);
    }

    private PolicyHeader mapHeader(ResultSet rs) throws SQLException {
        return new PolicyHeader(rs.getObject("policy_id", UUID.class),
                rs.getString("policy_code"), rs.getString("policy_name"), rs.getString("currency_code"),
                rs.getBigDecimal("minimum_amount"), rs.getBigDecimal("maximum_amount"),
                rs.getBoolean("is_active"), rs.getTimestamp("updated_at").toInstant());
    }

    private AwardApprovalPolicyResponse toResponse(PolicyHeader header) {
        List<AwardApprovalPolicyResponse.Stage> stages = jdbc.query("""
                SELECT u.user_id, u.full_name, u.email, s.sequence_number
                FROM srm.award_approval_policy_stage s
                JOIN srm.app_user u ON u.user_id = s.assigned_user_id
                WHERE s.policy_id = ? ORDER BY s.sequence_number
                """, (rs, row) -> new AwardApprovalPolicyResponse.Stage(
                rs.getObject("user_id", UUID.class), rs.getString("full_name"),
                rs.getString("email"), rs.getInt("sequence_number")), header.policyId());
        return new AwardApprovalPolicyResponse(header.policyId(), header.policyCode(),
                header.policyName(), header.currencyCode(), header.minimumAmount(),
                header.maximumAmount(), header.active(), stages, header.updatedAt());
    }

    private void audit(UUID actor, String action, UUID policyId, String reason) {
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id,
                     outcome, reason, effective_roles)
                VALUES (?, 'USER', ?, 'APPROVAL_POLICY', ?, 'SUCCESS', ?, ARRAY['SRM_ADMIN'])
                """, actor, action, policyId, reason);
    }

    private record PolicyHeader(UUID policyId, String policyCode, String policyName, String currencyCode,
                                BigDecimal minimumAmount, BigDecimal maximumAmount, boolean active,
                                java.time.Instant updatedAt) {}
    private record PolicyRange(BigDecimal minimumAmount, BigDecimal maximumAmount) {}
    private record PolicyRoute(UUID policyId, String policyCode, BigDecimal minimumAmount, BigDecimal maximumAmount) {}
    private record RecommendationAmount(UUID recommendationId, UUID recommenderId,
                                        BigDecimal amount, String currencyCode) {}
}
