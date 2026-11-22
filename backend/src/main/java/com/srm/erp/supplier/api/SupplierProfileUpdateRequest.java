package com.srm.erp.supplier.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupplierProfileUpdateRequest(
        @Size(max = 200) String legalName,
        @Size(max = 200) String tradeName,
        @Pattern(regexp = "^(?:[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z])?$",
                message = "Enter a valid GSTIN")
        String gstin,
        @Pattern(regexp = "^(?:[A-Z]{5}[0-9]{4}[A-Z])?$", message = "Enter a valid PAN")
        String pan,
        @Size(max = 160) String contactName,
        @Email @Size(max = 254) String email,
        @Size(max = 40) String phone,
        @Size(max = 200) String addressLine1,
        @Size(max = 100) String city,
        @Size(max = 100) String stateRegion,
        @Size(max = 20) String postalCode,
        @Size(min = 2, max = 2) String countryCode
) {}
