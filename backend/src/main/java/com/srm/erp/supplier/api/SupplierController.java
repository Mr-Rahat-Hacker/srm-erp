package com.srm.erp.supplier.api;

import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.supplier.service.SapVendorMappingService;
import com.srm.erp.supplier.service.SupplierLifecycleService;
import com.srm.erp.supplier.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {
    private final SupplierService supplierService;
    private final SupplierLifecycleService lifecycleService;
    private final SapVendorMappingService sapVendorMappingService;

    public SupplierController(SupplierService supplierService, SupplierLifecycleService lifecycleService,
                              SapVendorMappingService sapVendorMappingService) {
        this.supplierService = supplierService;
        this.lifecycleService = lifecycleService;
        this.sapVendorMappingService = sapVendorMappingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPPLIER_VERIFIER','SRM_ADMIN','SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupplierResponse> register(@Valid @RequestBody SupplierRegistrationRequest request) {
        return ApiResponse.success("Supplier registered successfully", supplierService.register(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_BUYER','SUPPLIER_VERIFIER','SRM_ADMIN','SUPER_ADMIN','AUDITOR')")
    public ApiResponse<List<SupplierResponse>> findAll() {
        return ApiResponse.success("Suppliers retrieved successfully", supplierService.findAll());
    }

    @GetMapping("/{supplierId}")
    @PreAuthorize("hasAnyRole('PROCUREMENT_BUYER','SUPPLIER_VERIFIER','SRM_ADMIN','SUPER_ADMIN','AUDITOR')")
    public ApiResponse<SupplierResponse> findById(@PathVariable UUID supplierId) {
        return ApiResponse.success("Supplier retrieved successfully", supplierService.findByIdPublic(supplierId));
    }

    @GetMapping("/{supplierId}/sap-vendor-mappings")
    @PreAuthorize("hasAnyRole('PROCUREMENT_BUYER','SUPPLIER_VERIFIER','SRM_ADMIN','SUPER_ADMIN','AUDITOR')")
    public ApiResponse<List<SapVendorMappingResponse>> findSapVendorMappings(@PathVariable UUID supplierId) {
        return ApiResponse.success("SAP Vendor Code mappings retrieved successfully",
                sapVendorMappingService.findForSupplier(supplierId));
    }

    @PutMapping("/{supplierId}/sap-vendor-mappings")
    @PreAuthorize("hasAnyRole('SUPPLIER_VERIFIER','SRM_ADMIN')")
    public ApiResponse<SapVendorMappingResponse> saveSapVendorMapping(
            @PathVariable UUID supplierId, @Valid @RequestBody SapVendorMappingRequest request) {
        return ApiResponse.success("SAP Vendor Code mapping saved successfully",
                sapVendorMappingService.save(supplierId, currentActor(), request));
    }

    @DeleteMapping("/{supplierId}/sap-vendor-mappings/{sapSystemCode}")
    @PreAuthorize("hasAnyRole('SUPPLIER_VERIFIER','SRM_ADMIN')")
    public ApiResponse<Void> deactivateSapVendorMapping(
            @PathVariable UUID supplierId, @PathVariable String sapSystemCode,
            @RequestParam String reason) {
        sapVendorMappingService.deactivate(supplierId, sapSystemCode, currentActor(), reason);
        return ApiResponse.success("SAP Vendor Code mapping deactivated successfully", null);
    }

    @PostMapping("/{supplierId}/submit")
    @PreAuthorize("hasRole('SRM_ADMIN')")
    public ApiResponse<SupplierLifecycleService.LifecycleResult> submit(@PathVariable UUID supplierId) {
        return ApiResponse.success("Supplier submitted for verification",
                lifecycleService.submit(supplierId, currentActor()));
    }

    @PostMapping("/{supplierId}/start-review")
    @PreAuthorize("hasAnyRole('SUPPLIER_VERIFIER','SRM_ADMIN')")
    public ApiResponse<SupplierLifecycleService.LifecycleResult> startReview(@PathVariable UUID supplierId) {
        return ApiResponse.success("Supplier review started",
                lifecycleService.startReview(supplierId, currentActor()));
    }

    @PostMapping("/{supplierId}/decision")
    @PreAuthorize("hasAnyRole('SUPPLIER_VERIFIER','SRM_ADMIN')")
    public ApiResponse<SupplierLifecycleService.LifecycleResult> decide(
            @PathVariable UUID supplierId, @Valid @RequestBody SupplierDecisionRequest request) {
        return ApiResponse.success("Supplier review decision recorded",
                lifecycleService.decide(supplierId, currentActor(), request.decision(), request.remarks()));
    }

    private UUID currentActor() {
        return UUID.fromString(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName());
    }
}
