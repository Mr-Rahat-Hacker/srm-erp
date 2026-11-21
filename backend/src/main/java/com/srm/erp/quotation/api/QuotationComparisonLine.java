package com.srm.erp.quotation.api;

import java.math.BigDecimal;
import java.util.UUID;

public record QuotationComparisonLine(
        UUID rfqLineId,
        BigDecimal basicUnitPrice,
        BigDecimal netUnitPrice,
        BigDecimal taxAmount,
        BigDecimal freightAmount,
        BigDecimal packingAmount,
        BigDecimal toolingAmount,
        BigDecimal insuranceAmount,
        BigDecimal otherCharges,
        BigDecimal landedLineTotal
) {}
