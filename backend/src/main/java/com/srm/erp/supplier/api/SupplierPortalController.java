package com.srm.erp.supplier.api;

import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.supplier.service.SupplierService;
import com.srm.erp.supplier.service.SupplierLifecycleService;
import com.srm.erp.supplier.service.SupplierProfileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/supplier")
@PreAuthorize("hasRole('SUPPLIER_USER')")
public class SupplierPortalController {
    private final SupplierService suppliers;
    private final SupplierLifecycleService lifecycle;
    private final SupplierProfileService profiles;

    public SupplierPortalController(SupplierService suppliers, SupplierLifecycleService lifecycle,
                                   SupplierProfileService profiles) {
        this.suppliers = suppliers;
        this.lifecycle = lifecycle;
        this.profiles = profiles;
    }

    @PostMapping("/profiles/{supplierId}/submit")
    public ApiResponse<SupplierLifecycleService.LifecycleResult> submitMyProfile(@PathVariable UUID supplierId) {
        UUID actorId = currentActor();
        return ApiResponse.success("Supplier profile submitted for verification",
                lifecycle.submitForSupplierUser(supplierId, actorId));
    }

    @PatchMapping("/profiles/{supplierId}")
    public ApiResponse<SupplierResponse> updateMyProfile(
            @PathVariable UUID supplierId, @Valid @RequestBody SupplierProfileUpdateRequest request) {
        return ApiResponse.success("Supplier profile updated successfully",
                profiles.updateForUser(supplierId, currentActor(), request));
    }

    @GetMapping("/profiles")
    public ApiResponse<List<SupplierResponse>> myProfiles() {
        return ApiResponse.success("Supplier profiles retrieved successfully", suppliers.findForUser(currentActor()));
    }

    private UUID currentActor() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
