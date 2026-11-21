package com.srm.erp.approval.api;

import java.time.Instant;
import java.util.UUID;

public record ApprovalTaskRecord(
        UUID approvalRequestId,
        UUID approvalTaskId,
        UUID rfqId,
        String rfqNumber,
        String rfqTitle,
        UUID recommendationId,
        UUID supplierId,
        String supplierNumber,
        String supplierName,
        UUID quotationVersionId,
        Integer versionNumber,
        String justification,
        UUID requestedBy,
        String requesterName,
        String status,
        Instant createdAt,
        Instant dueAt
) {}
