package com.srm.erp.quotation.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record QuotationDraftRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currencyCode,
        LocalDate validityDate,
        @Size(max = 4000) String paymentTerms,
        @Size(max = 20) String incoterms,
        @Digits(integer = 5, fraction = 0) Integer leadTimeDays,
        @Digits(integer = 14, fraction = 4) @DecimalMin("0.0001") BigDecimal minimumOrderQuantity,
        @Size(max = 2000) String warrantyTerms,
        @Size(max = 4000) String commercialRemarks,
        @Size(max = 4000) String technicalDeviations,
        @NotEmpty List<@Valid QuotationLineRequest> lines
) {}
