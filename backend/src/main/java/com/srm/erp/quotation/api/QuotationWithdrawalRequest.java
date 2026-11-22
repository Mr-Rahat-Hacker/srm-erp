package com.srm.erp.quotation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuotationWithdrawalRequest(
        @NotBlank @Size(max = 4000) String reason
) {}
