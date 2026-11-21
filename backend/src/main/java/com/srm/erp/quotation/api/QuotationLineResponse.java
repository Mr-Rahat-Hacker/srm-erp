package com.srm.erp.quotation.api;

import java.math.BigDecimal;
import java.util.UUID;

public record QuotationLineResponse(
        UUID rfqLineId,
        BigDecimal basicUnitPrice,
        BigDecimal discountAmount,
        BigDecimal netUnitPrice,
        BigDecimal taxAmount,
        BigDecimal freightAmount,
        BigDecimal packingAmount,
        BigDecimal toolingAmount,
        BigDecimal insuranceAmount,
        BigDecimal otherCharges,
        BigDecimal landedLineTotal
) {}
