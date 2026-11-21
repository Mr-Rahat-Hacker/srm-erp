package com.srm.erp.evaluation.api;

import java.util.List;
import java.util.UUID;

public record RfqEvaluationHistory(
        UUID rfqId,
        String rfqNumber,
        String title,
        String currencyCode,
        List<TechnicalEvaluationRecord> technicalEvaluations,
        List<CommercialEvaluationRecord> commercialEvaluations,
        List<SupplierRecommendationRecord> recommendations
) {}
