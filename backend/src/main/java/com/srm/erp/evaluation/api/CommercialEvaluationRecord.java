package com.srm.erp.evaluation.api;

import java.time.Instant;
import java.util.UUID;

public record CommercialEvaluationRecord(
        UUID supplierId,
        String supplierNumber,
        String supplierName,
        UUID quotationVersionId,
        int quotationVersion,
        UUID evaluatorUserId,
        String evaluatorName,
        String comparisonBasis,
        String remarks,
        Instant evaluatedAt
) {}
