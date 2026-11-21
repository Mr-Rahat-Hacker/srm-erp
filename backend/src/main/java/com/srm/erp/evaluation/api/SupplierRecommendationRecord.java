package com.srm.erp.evaluation.api;

import java.time.Instant;
import java.util.UUID;

public record SupplierRecommendationRecord(
        UUID recommendationId,
        UUID supplierId,
        String supplierNumber,
        String supplierName,
        UUID quotationVersionId,
        int quotationVersion,
        UUID recommendedBy,
        String recommenderName,
        boolean nonL1,
        String justification,
        Instant createdAt
) {}
