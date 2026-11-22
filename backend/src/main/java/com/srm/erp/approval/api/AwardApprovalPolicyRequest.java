package com.srm.erp.approval.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AwardApprovalPolicyRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Za-z0-9_-]+$")
        String policyCode,
        @NotBlank @Size(max = 160) String policyName,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$")
        String currencyCode,
        @NotNull @PositiveOrZero @Digits(integer = 16, fraction = 2)
        BigDecimal minimumAmount,
        @Positive @Digits(integer = 16, fraction = 2)
        BigDecimal maximumAmount,
        @NotNull @Size(min = 1, max = 10)
        List<@NotNull UUID> approverUserIds,
        @NotBlank @Size(max = 500) String reason
) {}
