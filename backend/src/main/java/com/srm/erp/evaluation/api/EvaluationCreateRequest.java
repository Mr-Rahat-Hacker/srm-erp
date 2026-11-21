package com.srm.erp.evaluation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EvaluationCreateRequest(
        @NotNull UUID supplierId,
        @NotBlank @Size(max = 35) String technicalStatus,
        @Size(max = 4000) String technicalRemarks,
        @NotBlank @Size(max = 4000) String commercialRemarks,
        boolean recommendSupplier,
        @Size(max = 4000) String recommendationJustification
) {}
