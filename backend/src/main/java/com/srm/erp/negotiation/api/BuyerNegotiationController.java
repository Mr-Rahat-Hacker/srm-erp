package com.srm.erp.negotiation.api;

import com.srm.erp.negotiation.service.NegotiationService;
import com.srm.erp.platform.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rfqs/{rfqId}/negotiations")
@PreAuthorize("hasRole('PROCUREMENT_BUYER')")
public class BuyerNegotiationController {
    private final NegotiationService negotiations;

    public BuyerNegotiationController(NegotiationService negotiations) {
        this.negotiations = negotiations;
    }

    @GetMapping
    public ApiResponse<List<NegotiationRoundResponse>> list(@PathVariable UUID rfqId) {
        return ApiResponse.success("Negotiation rounds retrieved successfully",
                negotiations.listForBuyer(rfqId, currentActor()));
    }

    @PostMapping
    public ApiResponse<List<NegotiationRoundResponse>> initiate(
            @PathVariable UUID rfqId, @Valid @RequestBody NegotiationCreateRequest request) {
        return ApiResponse.success("Negotiation requests sent to participating suppliers",
                negotiations.initiate(rfqId, currentActor(), request));
    }

    @PostMapping("/{roundId}/close")
    public ApiResponse<NegotiationRoundResponse> close(
            @PathVariable UUID rfqId, @PathVariable UUID roundId,
            @Valid @RequestBody NegotiationCloseRequest request) {
        return ApiResponse.success("Negotiation round closed successfully",
                negotiations.close(rfqId, roundId, currentActor(), request));
    }

    private UUID currentActor() {
        return UUID.fromString(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName());
    }
}
