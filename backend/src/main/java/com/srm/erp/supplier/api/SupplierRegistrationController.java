package com.srm.erp.supplier.api;

import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.supplier.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/supplier")
public class SupplierRegistrationController {
    private final SupplierService suppliers;

    public SupplierRegistrationController(SupplierService suppliers) {
        this.suppliers = suppliers;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupplierResponse> register(@Valid @RequestBody SupplierRegistrationRequest request) {
        return ApiResponse.success("Supplier account created. Sign in to continue supplier onboarding.",
                suppliers.registerSelf(request));
    }
}
