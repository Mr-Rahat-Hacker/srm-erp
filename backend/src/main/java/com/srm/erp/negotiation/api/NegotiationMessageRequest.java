package com.srm.erp.negotiation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record NegotiationMessageRequest(
        @NotBlank @Size(max = 4000) String message,
        UUID quotationVersionId
) {}
