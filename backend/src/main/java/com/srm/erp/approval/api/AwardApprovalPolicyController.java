package com.srm.erp.approval.api;

import com.srm.erp.approval.service.AwardApprovalPolicyService;
import com.srm.erp.platform.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approval-policies")
@PreAuthorize("hasAnyRole('SRM_ADMIN','SUPER_ADMIN')")
public class AwardApprovalPolicyController {
    private final AwardApprovalPolicyService policies;

    public AwardApprovalPolicyController(AwardApprovalPolicyService policies) {
        this.policies = policies;
    }

    @GetMapping
    public ApiResponse<List<AwardApprovalPolicyResponse>> findAll() {
        return ApiResponse.success("Award approval policies retrieved", policies.findAll());
    }

    @GetMapping("/eligible-approvers")
    public ApiResponse<List<EligibleApproverRecord>> eligibleApprovers() {
        return ApiResponse.success("Eligible approvers retrieved", policies.eligibleApprovers());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AwardApprovalPolicyResponse> create(
            @Valid @RequestBody AwardApprovalPolicyRequest request) {
        return ApiResponse.success("Award approval policy created", policies.create(actorId(), request));
    }

    @PutMapping("/{policyId}")
    public ApiResponse<AwardApprovalPolicyResponse> update(
            @PathVariable UUID policyId, @Valid @RequestBody AwardApprovalPolicyRequest request) {
        return ApiResponse.success("Award approval policy updated",
                policies.update(policyId, actorId(), request));
    }

    @DeleteMapping("/{policyId}")
    public ApiResponse<Void> deactivate(
            @PathVariable UUID policyId, @RequestParam String reason) {
        policies.deactivate(policyId, actorId(), reason);
        return ApiResponse.success("Award approval policy deactivated", null);
    }

    private UUID actorId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
