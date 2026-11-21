package com.srm.erp.evaluation.api;

import com.srm.erp.evaluation.service.QuotationEvaluationService;
import com.srm.erp.platform.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rfqs/{rfqId}/evaluations")
@PreAuthorize("hasRole('PROCUREMENT_BUYER')")
public class QuotationEvaluationController {
    private final QuotationEvaluationService evaluations;

    public QuotationEvaluationController(QuotationEvaluationService evaluations) {
        this.evaluations = evaluations;
    }

    @GetMapping
    public ApiResponse<RfqEvaluationHistory> history(@PathVariable UUID rfqId) {
        return ApiResponse.success("Evaluation history retrieved successfully",
                evaluations.history(rfqId, currentActor()));
    }

    @PostMapping
    public ApiResponse<RfqEvaluationHistory> evaluate(
            @PathVariable UUID rfqId, @Valid @RequestBody EvaluationCreateRequest request) {
        return ApiResponse.success("Technical and commercial evaluation recorded",
                evaluations.evaluate(rfqId, currentActor(), request));
    }

    private UUID currentActor() {
        return UUID.fromString(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName());
    }
}
