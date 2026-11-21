package com.srm.erp.quotation.api;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierRfqLine(
        UUID rfqLineId,
        int lineNumber,
        String sapMaterialCode,
        String description,
        BigDecimal quantity,
        String uomCode,
        String plantCode,
        String technicalSpecifications
) {}
