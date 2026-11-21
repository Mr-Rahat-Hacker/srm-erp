package com.srm.erp.supplier.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierDecisionRequest(
        @NotBlank String decision,
        @Size(max = 2000) String remarks
) {}
