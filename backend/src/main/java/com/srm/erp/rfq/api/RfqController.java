package com.srm.erp.rfq.api;

import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.rfq.service.RfqService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rfqs")
public class RfqController {
    private final RfqService rfqs;

    public RfqController(RfqService rfqs) {
        this.rfqs = rfqs;
    }

    @PostMapping
    @PreAuthorize("hasRole('PROCUREMENT_BUYER')")
    public ApiResponse<RfqResponse> create(@Valid @RequestBody RfqCreateRequest request) {
        return ApiResponse.success("RFQ draft created successfully", rfqs.create(currentActor(), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_BUYER','SUPPLIER_VERIFIER','SRM_ADMIN','SUPER_ADMIN','AUDITOR')")
    public ApiResponse<List<RfqSummaryResponse>> findAll(Authentication authentication) {
        return ApiResponse.success("RFQs retrieved successfully",
                rfqs.findAll(currentActor(), buyerOnly(authentication)));
    }

    @GetMapping("/{rfqId}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_BUYER','SUPPLIER_VERIFIER','SRM_ADMIN','SUPER_ADMIN','AUDITOR')")
    public ApiResponse<RfqResponse> findOne(@PathVariable UUID rfqId, Authentication authentication) {
        return ApiResponse.success("RFQ retrieved successfully",
                rfqs.findOne(rfqId, currentActor(), buyerOnly(authentication)));
    }

    @PostMapping("/{rfqId}/invitations")
    @PreAuthorize("hasRole('PROCUREMENT_BUYER')")
    public ApiResponse<List<RfqInvitationResponse>> invite(
            @PathVariable UUID rfqId,
            @Valid @RequestBody RfqInviteRequest request) {
        return ApiResponse.success("Qualified suppliers invited to RFQ",
                rfqs.invite(rfqId, currentActor(), request.supplierIds()));
    }

    @PostMapping("/{rfqId}/publish")
    @PreAuthorize("hasRole('PROCUREMENT_BUYER')")
    public ApiResponse<RfqResponse> publish(@PathVariable UUID rfqId) {
        return ApiResponse.success("RFQ published successfully",
                rfqs.publish(rfqId, currentActor()));
    }

    private UUID currentActor() {
        return UUID.fromString(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName());
    }

    private boolean buyerOnly(Authentication authentication) {
        boolean buyer = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PROCUREMENT_BUYER".equals(authority.getAuthority()));
        boolean broaderReadRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> List.of("ROLE_SUPPLIER_VERIFIER", "ROLE_SRM_ADMIN",
                        "ROLE_SUPER_ADMIN", "ROLE_AUDITOR").contains(authority.getAuthority()));
        return buyer && !broaderReadRole;
    }
}
