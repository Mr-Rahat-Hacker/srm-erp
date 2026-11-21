package com.srm.erp.quotation.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuotationComparisonResponse(
        UUID rfqId,
        String rfqNumber,
        String title,
        String currencyCode,
        Instant submissionDeadline,
        List<SupplierRfqLine> rfqLines,
        List<QuotationComparisonSupplier> suppliers
) {}
