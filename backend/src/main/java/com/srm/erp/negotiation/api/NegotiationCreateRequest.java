package com.srm.erp.negotiation.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NegotiationCreateRequest(
        @NotEmpty @Size(max = 200) List<@NotNull UUID> supplierIds,
        @NotNull @Future Instant responseDeadline,
        @NotBlank @Size(max = 4000) String initialRequest
) {}
