package com.srm.erp.approval.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(
        @NotBlank String decision,
        @Size(max = 4000) String comments
) {}
