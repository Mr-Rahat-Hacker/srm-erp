package com.srm.erp.supplier.api;

import java.time.Instant;
import java.util.UUID;

public record SupplierResponse(
        UUID supplierId,
        String supplierNumber,
        String legalName,
        String tradeName,
        String gstin,
        String pan,
        String lifecycleStatus,
        String contactName,
        String email,
        String phone,
        Instant createdAt
) {}
