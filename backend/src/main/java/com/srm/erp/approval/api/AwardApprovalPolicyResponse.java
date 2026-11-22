package com.srm.erp.approval.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AwardApprovalPolicyResponse(
        UUID policyId,
        String policyCode,
        String policyName,
        String currencyCode,
        BigDecimal minimumAmount,
        BigDecimal maximumAmount,
        boolean active,
        List<Stage> stages,
        Instant updatedAt
) {
    public record Stage(UUID userId, String fullName, String email, int sequenceNumber) {}
}
