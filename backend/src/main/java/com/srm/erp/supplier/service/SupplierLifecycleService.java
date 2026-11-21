package com.srm.erp.supplier.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SupplierLifecycleService {
    private final JdbcTemplate jdbc;

    public SupplierLifecycleService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public LifecycleResult submit(UUID supplierId, UUID actorId) {
        String current = currentStatus(supplierId);
        if (!"DRAFT".equals(current) && !"CORRECTION_REQUIRED".equals(current)) {
            throw new ResponseStatusException(CONFLICT, "Only draft or correction-required suppliers can be submitted");
        }
        return transition(supplierId, actorId, current, "SUBMITTED", "SUPPLIER_SUBMITTED", null, "SRM_ADMIN");
    }

    @Transactional
    public LifecycleResult startReview(UUID supplierId, UUID actorId) {
        String current = currentStatus(supplierId);
        if (!"SUBMITTED".equals(current)) {
            throw new ResponseStatusException(CONFLICT, "Only submitted suppliers can enter review");
        }
        return transition(supplierId, actorId, current, "UNDER_REVIEW", "REVIEW_STARTED", null, "SUPPLIER_VERIFIER");
    }

    @Transactional
    public LifecycleResult decide(UUID supplierId, UUID actorId, String decision, String remarks) {
        String current = currentStatus(supplierId);
        if (!"UNDER_REVIEW".equals(current)) {
            throw new ResponseStatusException(CONFLICT, "Supplier must be under review before a decision");
        }
        String next = switch (decision.trim().toUpperCase()) {
            case "APPROVE" -> "APPROVED";
            case "RETURN" -> "CORRECTION_REQUIRED";
            case "REJECT" -> "REJECTED";
            default -> throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Decision must be APPROVE, RETURN or REJECT");
        };
        if (("RETURN".equalsIgnoreCase(decision) || "REJECT".equalsIgnoreCase(decision))
                && (remarks == null || remarks.isBlank())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Remarks are required for return or rejection");
        }
        jdbc.update("""
                INSERT INTO srm.supplier_review
                    (supplier_id, reviewer_user_id, review_stage, decision, remarks)
                VALUES (?, ?, 'QUALIFICATION', ?, ?)
                """, supplierId, actorId, decision.trim().toUpperCase(), remarks);
        return transition(supplierId, actorId, current, next, "SUPPLIER_" + next,
                remarks, "SUPPLIER_VERIFIER");
    }

    private String currentStatus(UUID supplierId) {
        var statuses = jdbc.query("SELECT lifecycle_status FROM srm.supplier_organization WHERE supplier_id = ?",
                (rs, row) -> rs.getString(1), supplierId);
        if (statuses.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Supplier not found");
        return statuses.getFirst();
    }

    private LifecycleResult transition(UUID supplierId, UUID actorId, String oldStatus,
                                        String newStatus, String action, String reason, String actorType) {
        int updated = jdbc.update("""
                UPDATE srm.supplier_organization SET lifecycle_status = ?, updated_at = now()
                WHERE supplier_id = ? AND lifecycle_status = ?
                """, newStatus, supplierId, oldStatus);
        if (updated != 1) throw new ResponseStatusException(CONFLICT, "Supplier status changed; refresh and retry");
        jdbc.update("""
                INSERT INTO srm.supplier_status_history
                    (supplier_id, old_status, new_status, changed_by, reason)
                VALUES (?, ?, ?, ?, ?)
                """, supplierId, oldStatus, newStatus, actorId, reason);
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id,
                     outcome, reason, changed_fields)
                VALUES (?, ?, ?, 'SUPPLIER', ?, ?, 'SUCCESS', ?, 
                        jsonb_build_object('lifecycle_status', jsonb_build_object('old', ?, 'new', ?)))
                """, actorId, actorType, action, supplierId, supplierId, reason, oldStatus, newStatus);
        return new LifecycleResult(supplierId, oldStatus, newStatus, action);
    }

    public record LifecycleResult(UUID supplierId, String previousStatus, String currentStatus, String action) {}
}
