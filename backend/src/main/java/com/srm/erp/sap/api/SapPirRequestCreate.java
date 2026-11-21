package com.srm.erp.sap.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SapPirRequestCreate(
        @NotBlank @Size(max = 40) String sapSystemCode
) {}
