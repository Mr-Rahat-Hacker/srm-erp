package com.srm.erp.rfq.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record RfqInviteRequest(
        @NotEmpty @Size(max = 500) List<@NotNull UUID> supplierIds
) {}
