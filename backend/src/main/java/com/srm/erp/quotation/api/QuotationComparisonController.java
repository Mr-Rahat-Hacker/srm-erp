package com.srm.erp.quotation.api;

import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.quotation.service.QuotationComparisonService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rfqs/{rfqId}/quotations")
@PreAuthorize("hasRole('PROCUREMENT_BUYER')")
public class QuotationComparisonController {
    private final QuotationComparisonService comparisons;

    public QuotationComparisonController(QuotationComparisonService comparisons) {
        this.comparisons = comparisons;
    }

    @GetMapping
    public ApiResponse<QuotationComparisonResponse> compare(@PathVariable UUID rfqId) {
        UUID actor = UUID.fromString(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName());
        return ApiResponse.success("Submitted quotations retrieved for comparison",
                comparisons.compare(rfqId, actor));
    }
}
