package com.srm.erp.evaluation.api;

import java.time.Instant;
import java.util.UUID;

public record TechnicalEvaluationRecord(
        UUID supplierId,
        String supplierNumber,
        String supplierName,
        UUID quotationVersionId,
        int quotationVersion,
        UUID evaluatorUserId,
        String evaluatorName,
        String technicalStatus,
        String remarks,
        Instant evaluatedAt
) {}
