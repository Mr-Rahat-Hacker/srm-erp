package com.srm.erp.negotiation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NegotiationCloseRequest(
        @NotBlank @Size(max = 25) String outcome,
        @Size(max = 2000) String remarks
) {}
