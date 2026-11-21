package com.srm.erp.rfq.api;

import java.math.BigDecimal;
import java.util.UUID;

public record RfqLineResponse(
        UUID rfqLineId,
        int lineNumber,
        String sapMaterialCode,
        String description,
        String drawingRevision,
        BigDecimal quantity,
        String uomCode,
        String plantCode,
        String technicalSpecifications
) {}
