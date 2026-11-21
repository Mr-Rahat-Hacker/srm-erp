package com.srm.erp.quotation.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuotationComparisonSupplier(
        UUID supplierId,
        String supplierNumber,
        String legalName,
        String invitationStatus,
        Integer submittedVersion,
        Instant submittedAt,
        BigDecimal totalLandedCost,
        Integer leadTimeDays,
        LocalDate validityDate,
        String paymentTerms,
        String incoterms,
        String commercialRemarks,
        String technicalDeviations,
        List<QuotationComparisonLine> lines
) {}
