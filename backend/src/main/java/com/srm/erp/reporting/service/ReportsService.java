package com.srm.erp.reporting.service;

import com.srm.erp.reporting.api.ReportsDashboard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReportsService {
    private static final Set<String> INTERNAL_ROLES = Set.of("SUPPLIER_VERIFIER", "PROCUREMENT_BUYER",
            "PROCUREMENT_APPROVER", "SRM_ADMIN", "SUPER_ADMIN", "AUDITOR");
    private final JdbcTemplate jdbc;

    public ReportsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ReportsDashboard dashboard(UUID actorId, Set<String> roles) {
        boolean supplierOnly = roles.contains("SUPPLIER_USER")
                && roles.stream().noneMatch(INTERNAL_ROLES::contains);
        if (supplierOnly) return supplierDashboard(actorId);

        boolean allRfqs = roles.contains("AUDITOR") || roles.contains("SRM_ADMIN")
                || roles.contains("SUPER_ADMIN");
        Long invitations = count("""
                SELECT count(*) FROM srm.rfq_invitation i
                WHERE ? OR EXISTS (SELECT 1 FROM srm.rfq r
                                   WHERE r.rfq_id = i.rfq_id AND r.buyer_user_id = ?)
                """, allRfqs, actorId);
        Long activeRfqs = count("""
                SELECT count(*) FROM srm.rfq r
                WHERE (? OR r.buyer_user_id = ? OR EXISTS (
                    SELECT 1 FROM srm.approval_task t
                    JOIN srm.approval_request ar ON ar.approval_request_id = t.approval_request_id
                    JOIN srm.supplier_recommendation sr ON sr.recommendation_id = ar.entity_id
                    WHERE ar.entity_type = 'AWARD' AND sr.rfq_id = r.rfq_id AND t.assigned_user_id = ?))
                  AND r.status NOT IN ('AWARDED','CLOSED','CANCELLED')
                """, allRfqs, actorId, actorId);
        Long submitted = count("""
                SELECT count(DISTINCT q.quotation_id)
                FROM srm.quotation q
                JOIN srm.quotation_version v ON v.quotation_id = q.quotation_id
                JOIN srm.rfq r ON r.rfq_id = q.rfq_id
                WHERE v.version_status = 'SUBMITTED'
                  AND (? OR r.buyer_user_id = ? OR EXISTS (
                      SELECT 1 FROM srm.approval_task t
                      JOIN srm.approval_request ar ON ar.approval_request_id = t.approval_request_id
                      JOIN srm.supplier_recommendation sr ON sr.recommendation_id = ar.entity_id
                      WHERE ar.entity_type = 'AWARD' AND sr.rfq_id = r.rfq_id AND t.assigned_user_id = ?))
                """, allRfqs, actorId, actorId);
        Long negotiations = count("""
                SELECT count(*) FROM srm.negotiation_round n
                JOIN srm.rfq r ON r.rfq_id = n.rfq_id
                WHERE n.status = 'OPEN'
                  AND (? OR r.buyer_user_id = ? OR EXISTS (
                      SELECT 1 FROM srm.approval_task t
                      JOIN srm.approval_request ar ON ar.approval_request_id = t.approval_request_id
                      JOIN srm.supplier_recommendation sr ON sr.recommendation_id = ar.entity_id
                      WHERE ar.entity_type = 'AWARD' AND sr.rfq_id = r.rfq_id AND t.assigned_user_id = ?))
                """, allRfqs, actorId, actorId);
        Long pending = count("""
                SELECT count(DISTINCT ar.approval_request_id)
                FROM srm.approval_request ar
                JOIN srm.approval_task t ON t.approval_request_id = ar.approval_request_id
                LEFT JOIN srm.supplier_recommendation sr
                  ON ar.entity_type = 'AWARD' AND sr.recommendation_id = ar.entity_id
                LEFT JOIN srm.rfq r ON r.rfq_id = sr.rfq_id
                WHERE ar.status = 'PENDING'
                  AND (? OR t.assigned_user_id = ? OR r.buyer_user_id = ?)
                """, allRfqs, actorId, actorId);
        Long awards = count("""
                SELECT count(*) FROM srm.award a
                JOIN srm.rfq r ON r.rfq_id = a.rfq_id
                WHERE ? OR r.buyer_user_id = ? OR EXISTS (
                    SELECT 1 FROM srm.approval_task t
                    JOIN srm.approval_request ar ON ar.approval_request_id = t.approval_request_id
                    JOIN srm.supplier_recommendation sr ON sr.recommendation_id = ar.entity_id
                    WHERE ar.entity_type = 'AWARD' AND sr.rfq_id = r.rfq_id AND t.assigned_user_id = ?)
                """, allRfqs, actorId, actorId);
        return new ReportsDashboard("INTERNAL", invitations, activeRfqs, submitted, negotiations,
                pending, awards, supplierStatuses(), rfqStatuses(actorId, allRfqs));
    }

    private ReportsDashboard supplierDashboard(UUID actorId) {
        Long invitations = count("""
                SELECT count(*) FROM srm.rfq_invitation i
                JOIN srm.supplier_user_membership m ON m.supplier_id = i.supplier_id
                WHERE m.user_id = ? AND m.membership_status = 'ACTIVE'
                """, actorId);
        Long activeRfqs = count("""
                SELECT count(DISTINCT r.rfq_id) FROM srm.rfq r
                JOIN srm.rfq_invitation i ON i.rfq_id = r.rfq_id
                JOIN srm.supplier_user_membership m ON m.supplier_id = i.supplier_id
                WHERE m.user_id = ? AND m.membership_status = 'ACTIVE'
                  AND r.status NOT IN ('AWARDED','CLOSED','CANCELLED')
                """, actorId);
        Long submitted = count("""
                SELECT count(DISTINCT q.quotation_id) FROM srm.quotation q
                JOIN srm.quotation_version v ON v.quotation_id = q.quotation_id
                JOIN srm.supplier_user_membership m ON m.supplier_id = q.supplier_id
                WHERE m.user_id = ? AND m.membership_status = 'ACTIVE'
                  AND v.version_status = 'SUBMITTED'
                """, actorId);
        Long negotiations = count("""
                SELECT count(*) FROM srm.negotiation_round n
                JOIN srm.supplier_user_membership m ON m.supplier_id = n.supplier_id
                WHERE m.user_id = ? AND m.membership_status = 'ACTIVE' AND n.status = 'OPEN'
                """, actorId);
        Long awards = count("""
                SELECT count(*) FROM srm.award a
                JOIN srm.supplier_user_membership m ON m.supplier_id = a.supplier_id
                WHERE m.user_id = ? AND m.membership_status = 'ACTIVE'
                """, actorId);
        Map<String, Long> statuses = new LinkedHashMap<>();
        jdbc.query("""
                SELECT r.status, count(DISTINCT r.rfq_id) AS total
                FROM srm.rfq r
                JOIN srm.rfq_invitation i ON i.rfq_id = r.rfq_id
                JOIN srm.supplier_user_membership m ON m.supplier_id = i.supplier_id
                WHERE m.user_id = ? AND m.membership_status = 'ACTIVE'
                GROUP BY r.status ORDER BY r.status
                """, rs -> { statuses.put(rs.getString("status"), rs.getLong("total")); }, actorId);
        return new ReportsDashboard("SUPPLIER", invitations, activeRfqs, submitted, negotiations,
                0, awards, Map.of(), statuses);
    }

    private Map<String, Long> supplierStatuses() {
        Map<String, Long> statuses = new LinkedHashMap<>();
        jdbc.query("""
                SELECT lifecycle_status, count(*) AS total
                FROM srm.supplier_organization GROUP BY lifecycle_status ORDER BY lifecycle_status
                """, rs -> { statuses.put(rs.getString("lifecycle_status"), rs.getLong("total")); });
        return statuses;
    }

    private Map<String, Long> rfqStatuses(UUID actorId, boolean allRfqs) {
        Map<String, Long> statuses = new LinkedHashMap<>();
        jdbc.query("""
                SELECT r.status, count(*) AS total
                FROM srm.rfq r
                WHERE (? OR r.buyer_user_id = ? OR EXISTS (
                    SELECT 1 FROM srm.approval_task t
                    JOIN srm.approval_request ar ON ar.approval_request_id = t.approval_request_id
                    JOIN srm.supplier_recommendation sr ON sr.recommendation_id = ar.entity_id
                    WHERE ar.entity_type = 'AWARD' AND sr.rfq_id = r.rfq_id AND t.assigned_user_id = ?))
                GROUP BY r.status ORDER BY r.status
                """, rs -> { statuses.put(rs.getString("status"), rs.getLong("total")); },
                allRfqs, actorId, actorId);
        return statuses;
    }

    private Long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }
}
