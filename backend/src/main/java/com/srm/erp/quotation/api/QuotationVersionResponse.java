package com.srm.erp.quotation.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuotationVersionResponse(
        UUID quotationVersionId,
        int versionNumber,
        String versionStatus,
        String currencyCode,
        LocalDate validityDate,
        String paymentTerms,
        String incoterms,
        Integer leadTimeDays,
        BigDecimal minimumOrderQuantity,
        String warrantyTerms,
        String commercialRemarks,
        String technicalDeviations,
        BigDecimal totalLandedCost,
        Instant submittedAt,
        List<QuotationLineResponse> lines
) {}
