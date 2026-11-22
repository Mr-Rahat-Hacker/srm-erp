package com.srm.erp.approval.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AwardApprovalRequest(
        @NotNull UUID recommendationId,
        UUID approverUserId,
        @Size(max = 10) List<@NotNull UUID> approverUserIds
) {
    public AwardApprovalRequest(UUID recommendationId, UUID approverUserId) {
        this(recommendationId, approverUserId, List.of());
    }
}
