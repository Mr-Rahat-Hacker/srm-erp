package com.srm.erp.rfq.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RfqLineRequest(
        @NotBlank @Size(max = 80) String sapMaterialCode,
        @NotBlank @Size(max = 2000) String description,
        @Size(max = 60) String drawingRevision,
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 14, fraction = 4) BigDecimal quantity,
        @NotBlank @Size(max = 12) String uomCode,
        @Size(max = 30) String plantCode,
        @Size(max = 4000) String technicalSpecifications
) {}
