package com.srm.erp.negotiation.api;

import com.srm.erp.negotiation.service.NegotiationService;
import com.srm.erp.platform.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/supplier/negotiations")
@PreAuthorize("hasRole('SUPPLIER_USER')")
public class SupplierNegotiationController {
    private final NegotiationService negotiations;

    public SupplierNegotiationController(NegotiationService negotiations) {
        this.negotiations = negotiations;
    }

    @GetMapping
    public ApiResponse<List<NegotiationRoundResponse>> list() {
        return ApiResponse.success("Your negotiation requests retrieved successfully",
                negotiations.listForSupplier(currentActor()));
    }

    @PostMapping("/{roundId}/messages")
    public ApiResponse<NegotiationRoundResponse> reply(
            @PathVariable UUID roundId, @Valid @RequestBody NegotiationMessageRequest request) {
        return ApiResponse.success("Negotiation response recorded successfully",
                negotiations.reply(roundId, currentActor(), request));
    }

    private UUID currentActor() {
        return UUID.fromString(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName());
    }
}
