package com.srm.erp.audit.service;

import com.srm.erp.audit.api.AuditEventRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuditSearchService {
    private final JdbcTemplate jdbc;

    public AuditSearchService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public List<AuditEventRecord> search(UUID actorId, Set<String> roles, String action,
                                         String entityType, UUID rfqId, UUID targetActor,
                                         Instant from, Instant to, int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 200));
        boolean fullAccess = roles.contains("AUDITOR") || roles.contains("SRM_ADMIN")
                || roles.contains("SUPER_ADMIN");
        List<AuditEventRecord> events = jdbc.query("""
                SELECT e.audit_event_id, e.occurred_at, e.actor_user_id, u.full_name AS actor_name,
                       array_to_string(e.effective_roles, ',') AS effective_roles, e.action_code,
                       e.entity_type, e.entity_id, e.supplier_id, s.legal_name AS supplier_name,
                       e.rfq_id, r.rfq_number, e.outcome, e.reason, e.changed_fields::text AS changed_fields
                FROM srm.audit_event e
                LEFT JOIN srm.app_user u ON u.user_id = e.actor_user_id
                LEFT JOIN srm.supplier_organization s ON s.supplier_id = e.supplier_id
                LEFT JOIN srm.rfq r ON r.rfq_id = e.rfq_id
                WHERE (? OR e.actor_user_id = ?
                       OR EXISTS (SELECT 1 FROM srm.rfq owned WHERE owned.rfq_id = e.rfq_id
                                  AND owned.buyer_user_id = ?))
                  AND (CAST(? AS text) IS NULL OR e.action_code ILIKE '%' || ? || '%')
                  AND (CAST(? AS text) IS NULL OR e.entity_type = ?)
                  AND (CAST(? AS uuid) IS NULL OR e.rfq_id = CAST(? AS uuid))
                  AND (CAST(? AS uuid) IS NULL OR e.actor_user_id = CAST(? AS uuid))
                  AND (CAST(? AS timestamptz) IS NULL OR e.occurred_at >= CAST(? AS timestamptz))
                  AND (CAST(? AS timestamptz) IS NULL OR e.occurred_at < CAST(? AS timestamptz))
                ORDER BY e.occurred_at DESC
                LIMIT ?
                """, (rs, row) -> map(rs), fullAccess, actorId, actorId,
                blankToNull(action), blankToNull(action), blankToNull(entityType), blankToNull(entityType),
                rfqId, rfqId, targetActor, targetActor, timestamp(from), timestamp(from),
                timestamp(to), timestamp(to), limit);

        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, effective_roles, action_code, entity_type,
                     outcome, changed_fields)
                VALUES (?, 'USER', ?, 'AUDIT_SEARCH', 'AUDIT_EVENT', 'SUCCESS',
                        jsonb_build_object('action', ?, 'entityType', ?, 'rfqId', ?,
                                           'targetActor', ?, 'from', ?, 'to', ?, 'resultCount', ?))
                """, actorId, roles.toArray(String[]::new), blankToNull(action),
                blankToNull(entityType), rfqId, targetActor, from, to, events.size());
        return events;
    }

    private AuditEventRecord map(ResultSet rs) throws SQLException {
        Timestamp occurredAt = rs.getTimestamp("occurred_at");
        return new AuditEventRecord(rs.getObject("audit_event_id", UUID.class), occurredAt.toInstant(),
                rs.getObject("actor_user_id", UUID.class), rs.getString("actor_name"),
                rs.getString("effective_roles"), rs.getString("action_code"), rs.getString("entity_type"),
                rs.getObject("entity_id", UUID.class), rs.getObject("supplier_id", UUID.class),
                rs.getString("supplier_name"), rs.getObject("rfq_id", UUID.class),
                rs.getString("rfq_number"), rs.getString("outcome"), rs.getString("reason"),
                rs.getString("changed_fields"));
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
