package com.srm.erp.supplier.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SapVendorMappingRequest(
        @NotBlank @Size(max = 32)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Use letters, numbers, hyphen or underscore")
        String sapSystemCode,
        @NotBlank @Size(max = 40)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Use letters, numbers, hyphen or underscore")
        String sapVendorCode,
        @NotBlank @Size(max = 500) String reason
) {}
