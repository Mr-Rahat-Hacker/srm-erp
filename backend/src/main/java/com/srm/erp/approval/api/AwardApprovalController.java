package com.srm.erp.approval.api;

import com.srm.erp.approval.service.AwardApprovalService;
import com.srm.erp.platform.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AwardApprovalController {
    private final AwardApprovalService approvals;

    public AwardApprovalController(AwardApprovalService approvals) {
        this.approvals = approvals;
    }

    @PostMapping("/rfqs/{rfqId}/award-approvals")
    @PreAuthorize("hasRole('PROCUREMENT_BUYER')")
    public ApiResponse<ApprovalTaskRecord> requestApproval(
            @PathVariable UUID rfqId, @Valid @RequestBody AwardApprovalRequest request) {
        return ApiResponse.success("Award approval requested",
                approvals.requestApproval(rfqId, actorId(), request));
    }

    @GetMapping("/approvals/eligible-approvers")
    @PreAuthorize("hasRole('PROCUREMENT_BUYER')")
    public ApiResponse<List<EligibleApproverRecord>> eligibleApprovers() {
        return ApiResponse.success("Eligible approvers retrieved", approvals.eligibleApprovers(actorId()));
    }

    @GetMapping("/rfqs/{rfqId}/award-approvals")
    @PreAuthorize("hasRole('PROCUREMENT_BUYER')")
    public ApiResponse<List<ApprovalTaskRecord>> rfqApprovalRequests(@PathVariable UUID rfqId) {
        return ApiResponse.success("RFQ award approval requests retrieved",
                approvals.rfqRequests(rfqId, actorId()));
    }

    @GetMapping("/approvals/my-tasks")
    @PreAuthorize("hasRole('PROCUREMENT_APPROVER')")
    public ApiResponse<List<ApprovalTaskRecord>> myTasks() {
        return ApiResponse.success("Assigned approval tasks retrieved", approvals.myTasks(actorId()));
    }

    @PostMapping("/approvals/tasks/{taskId}/decision")
    @PreAuthorize("hasRole('PROCUREMENT_APPROVER')")
    public ApiResponse<ApprovalTaskRecord> decide(
            @PathVariable UUID taskId, @Valid @RequestBody ApprovalDecisionRequest request) {
        return ApiResponse.success("Approval decision recorded",
                approvals.decide(taskId, actorId(), request));
    }

    @PostMapping("/rfqs/{rfqId}/award")
    @PreAuthorize("hasRole('PROCUREMENT_BUYER')")
    public ApiResponse<AwardResponse> award(
            @PathVariable UUID rfqId, @RequestParam UUID approvalRequestId) {
        return ApiResponse.success("Final RFQ award recorded",
                approvals.award(rfqId, actorId(), approvalRequestId));
    }

    private UUID actorId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
