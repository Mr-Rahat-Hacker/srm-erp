package com.srm.erp.audit.service;

import com.srm.erp.audit.api.AuditEventRecord;
import com.srm.erp.platform.response.PageResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

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
        return searchPage(actorId, roles, action, entityType, rfqId, targetActor, from, to, 0, limit).items();
    }

    @Transactional
    public PageResponse<AuditEventRecord> searchPage(UUID actorId, Set<String> roles, String action,
                                                      String entityType, UUID rfqId, UUID targetActor,
                                                      Instant from, Instant to, int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 200");
        }
        boolean fullAccess = roles.contains("AUDITOR") || roles.contains("SRM_ADMIN")
                || roles.contains("SUPER_ADMIN");
        String where = """
                WHERE (? OR e.actor_user_id = ?
                       OR EXISTS (SELECT 1 FROM srm.rfq owned WHERE owned.rfq_id = e.rfq_id
                                  AND owned.buyer_user_id = ?))
                  AND (CAST(? AS text) IS NULL OR e.action_code ILIKE '%' || ? || '%')
                  AND (CAST(? AS text) IS NULL OR e.entity_type = ?)
                  AND (CAST(? AS uuid) IS NULL OR e.rfq_id = CAST(? AS uuid))
                  AND (CAST(? AS uuid) IS NULL OR e.actor_user_id = CAST(? AS uuid))
                  AND (CAST(? AS timestamptz) IS NULL OR e.occurred_at >= CAST(? AS timestamptz))
                  AND (CAST(? AS timestamptz) IS NULL OR e.occurred_at < CAST(? AS timestamptz))
                """;
        Object[] filters = {
                fullAccess, actorId, actorId,
                blankToNull(action), blankToNull(action),
                blankToNull(entityType), blankToNull(entityType),
                rfqId, rfqId, targetActor, targetActor,
                timestamp(from), timestamp(from), timestamp(to), timestamp(to)
        };
        Long total = jdbc.queryForObject("""
                SELECT count(*) FROM srm.audit_event e
                """ + where, Long.class, filters);
        List<AuditEventRecord> events = jdbc.query("""
                SELECT e.audit_event_id, e.occurred_at, e.actor_user_id, u.full_name AS actor_name,
                       array_to_string(e.effective_roles, ',') AS effective_roles, e.action_code,
                       e.entity_type, e.entity_id, e.supplier_id, s.legal_name AS supplier_name,
                       e.rfq_id, r.rfq_number, e.outcome, e.reason, e.changed_fields::text AS changed_fields
                FROM srm.audit_event e
                LEFT JOIN srm.app_user u ON u.user_id = e.actor_user_id
                LEFT JOIN srm.supplier_organization s ON s.supplier_id = e.supplier_id
                LEFT JOIN srm.rfq r ON r.rfq_id = e.rfq_id
                """ + where + """
                ORDER BY e.occurred_at DESC, e.audit_event_id DESC
                LIMIT ? OFFSET ?
                """, this::map, pageArguments(filters, size, (long) page * size));

        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, effective_roles, action_code, entity_type,
                     outcome, changed_fields)
                VALUES (?, 'USER', string_to_array(?, ','), 'AUDIT_SEARCH', 'AUDIT_EVENT', 'SUCCESS',
                        jsonb_build_object('action', ?, 'entityType', ?, 'rfqId', ?,
                                           'targetActor', ?, 'from', ?, 'to', ?, 'resultCount', ?))
                """, actorId, String.join(",", roles), blankToNull(action),
                blankToNull(entityType), rfqId, targetActor, from, to, total == null ? 0 : total);
        return PageResponse.of(events, page, size, total == null ? 0 : total);
    }

    private Object[] pageArguments(Object[] filters, int size, long offset) {
        Object[] args = Arrays.copyOf(filters, filters.length + 2);
        args[filters.length] = size;
        args[filters.length + 1] = offset;
        return args;
    }

    private AuditEventRecord map(ResultSet rs, int row) throws SQLException {
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
