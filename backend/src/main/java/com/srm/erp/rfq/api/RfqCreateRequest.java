package com.srm.erp.rfq.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RfqCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10000) String description,
        @Size(max = 60) String projectCode,
        @Size(max = 100) String department,
        @Size(max = 30) String plantCode,
        @Size(max = 30) String purchasingGroup,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currencyCode,
        @NotNull @Future Instant submissionDeadline,
        @FutureOrPresent LocalDate requiredDeliveryDate,
        @Size(max = 4000) String paymentTerms,
        @Size(max = 20) String incoterms,
        @NotEmpty List<@Valid RfqLineRequest> lines
) {}
