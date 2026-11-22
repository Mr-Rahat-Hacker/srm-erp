package com.srm.erp.supplier.api;

import java.util.UUID;

public record SapVendorMappingResponse(
        UUID supplierId,
        String sapSystemCode,
        String sapVendorCode,
        boolean active
) {}
