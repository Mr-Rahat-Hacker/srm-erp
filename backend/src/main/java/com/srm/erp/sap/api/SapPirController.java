package com.srm.erp.sap.api;

import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.sap.service.SapPirOutboxService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SapPirController {
    private final SapPirOutboxService sapPir;

    public SapPirController(SapPirOutboxService sapPir) {
        this.sapPir = sapPir;
    }

    @GetMapping("/awards/{awardId}/sap-pir")
    @PreAuthorize("hasRole('PROCUREMENT_BUYER')")
    public ApiResponse<List<SapIntegrationRecord>> forAward(@PathVariable UUID awardId) {
        return ApiResponse.success("SAP PIR integration requests retrieved",
                sapPir.forAward(awardId, actorId()));
    }

    @PostMapping("/awards/{awardId}/sap-pir")
    @PreAuthorize("hasRole('PROCUREMENT_BUYER')")
    public ApiResponse<SapIntegrationRecord> create(
            @PathVariable UUID awardId, @Valid @RequestBody SapPirRequestCreate request) {
        return ApiResponse.success("SAP PIR request queued for the approved integration gateway",
                sapPir.create(awardId, actorId(), request));
    }

    @PostMapping("/sap-integration-events/{eventId}/retry")
    @PreAuthorize("hasRole('PROCUREMENT_BUYER')")
    public ApiResponse<SapIntegrationRecord> retry(@PathVariable UUID eventId) {
        return ApiResponse.success("SAP PIR retry queued",
                sapPir.retry(eventId, actorId()));
    }

    private UUID actorId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
