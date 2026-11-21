package com.srm.erp.supplier.api;

import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.supplier.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {
    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupplierResponse> register(@Valid @RequestBody SupplierRegistrationRequest request) {
        return ApiResponse.success("Supplier registered successfully", supplierService.register(request));
    }

    @GetMapping
    public ApiResponse<List<SupplierResponse>> findAll() {
        return ApiResponse.success("Suppliers retrieved successfully", supplierService.findAll());
    }
}
