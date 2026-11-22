package com.srm.erp.quotation.api;

import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.quotation.service.SupplierQuotationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/supplier")
@PreAuthorize("hasRole('SUPPLIER_USER')")
public class SupplierQuotationController {
    private final SupplierQuotationService quotations;

    public SupplierQuotationController(SupplierQuotationService quotations) {
        this.quotations = quotations;
    }

    @GetMapping("/rfqs")
    public ApiResponse<List<SupplierRfqSummary>> findInvitedRfqs() {
        return ApiResponse.success("Invited RFQs retrieved successfully",
                quotations.findInvitedRfqs(currentActor()));
    }

    @GetMapping("/rfqs/{rfqId}")
    public ApiResponse<SupplierRfqResponse> findRfq(@PathVariable UUID rfqId) {
        return ApiResponse.success("RFQ and quotation history retrieved successfully",
                quotations.findRfq(rfqId, currentActor()));
    }

    @PutMapping("/rfqs/{rfqId}/quotation")
    public ApiResponse<SupplierRfqResponse> saveDraft(
            @PathVariable UUID rfqId, @Valid @RequestBody QuotationDraftRequest request) {
        return ApiResponse.success("Quotation draft saved successfully",
                quotations.saveDraft(rfqId, currentActor(), request));
    }

    @PostMapping("/rfqs/{rfqId}/quotation/submit")
    public ApiResponse<SupplierRfqResponse> submit(@PathVariable UUID rfqId) {
        return ApiResponse.success("Quotation version submitted successfully",
                quotations.submit(rfqId, currentActor()));
    }

    @PostMapping("/rfqs/{rfqId}/quotation/withdraw")
    public ApiResponse<SupplierRfqResponse> withdraw(
            @PathVariable UUID rfqId, @Valid @RequestBody QuotationWithdrawalRequest request) {
        return ApiResponse.success("Quotation withdrawn successfully",
                quotations.withdraw(rfqId, currentActor(), request));
    }

    private UUID currentActor() {
        return UUID.fromString(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName());
    }
}
