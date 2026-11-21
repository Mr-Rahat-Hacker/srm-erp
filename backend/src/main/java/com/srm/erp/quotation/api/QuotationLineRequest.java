package com.srm.erp.quotation.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record QuotationLineRequest(
        @NotNull UUID rfqLineId,
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 14, fraction = 4) BigDecimal basicUnitPrice,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal discountAmount,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal taxAmount,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal freightAmount,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal packingAmount,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal toolingAmount,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal insuranceAmount,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal otherCharges
) {}
