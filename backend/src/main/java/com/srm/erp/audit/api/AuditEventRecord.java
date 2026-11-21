package com.srm.erp.audit.api;

import java.time.Instant;
import java.util.UUID;

public record AuditEventRecord(
        UUID eventId,
        Instant occurredAt,
        UUID actorUserId,
        String actorName,
        String effectiveRoles,
        String actionCode,
        String entityType,
        UUID entityId,
        UUID supplierId,
        String supplierName,
        UUID rfqId,
        String rfqNumber,
        String outcome,
        String reason,
        String changedFields
) {}
