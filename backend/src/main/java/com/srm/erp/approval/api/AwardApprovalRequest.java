package com.srm.erp.approval.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AwardApprovalRequest(
        @NotNull UUID recommendationId,
        @NotNull UUID approverUserId
) {}
