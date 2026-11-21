package com.srm.erp.negotiation.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NegotiationRoundResponse(
        UUID negotiationRoundId,
        UUID rfqId,
        String rfqNumber,
        String rfqTitle,
        UUID supplierId,
        String supplierNumber,
        String supplierName,
        int roundNumber,
        String status,
        Instant openedAt,
        Instant responseDeadline,
        Instant closedAt,
        String outcome,
        String outcomeRemarks,
        Integer baselineQuotationVersion,
        List<NegotiationMessageResponse> messages
) {}
