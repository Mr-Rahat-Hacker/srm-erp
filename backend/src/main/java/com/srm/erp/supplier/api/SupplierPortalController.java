package com.srm.erp.supplier.api;

import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.supplier.service.SupplierService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/supplier")
@PreAuthorize("hasRole('SUPPLIER_USER')")
public class SupplierPortalController {
    private final SupplierService suppliers;

    public SupplierPortalController(SupplierService suppliers) {
        this.suppliers = suppliers;
    }

    @GetMapping("/profiles")
    public ApiResponse<List<SupplierResponse>> myProfiles() {
        UUID actorId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        return ApiResponse.success("Supplier profiles retrieved successfully", suppliers.findForUser(actorId));
    }
}
