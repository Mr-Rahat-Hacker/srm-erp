package com.srm.erp.supplier.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupplierRegistrationRequest(
        @NotBlank @Size(max = 200) String legalName,
        @Size(max = 200) String tradeName,
        @Pattern(regexp = "^(?:[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z])?$", message = "Enter a valid GSTIN")
        String gstin,
        @Pattern(regexp = "^(?:[A-Z]{5}[0-9]{4}[A-Z])?$", message = "Enter a valid PAN")
        String pan,
        @NotBlank @Size(max = 160) String contactName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 40) String phone,
        @Size(max = 200) String addressLine1,
        @Size(max = 100) String city,
        @Size(max = 100) String stateRegion,
        @Size(max = 20) String postalCode,
        @Size(min = 2, max = 2) String countryCode,
        @Size(max = 128) String password
) {
    public SupplierRegistrationRequest(String legalName, String tradeName, String gstin, String pan,
                                       String contactName, String email, String phone, String addressLine1,
                                       String city, String stateRegion, String postalCode, String countryCode) {
        this(legalName, tradeName, gstin, pan, contactName, email, phone, addressLine1, city,
                stateRegion, postalCode, countryCode, null);
    }
}
