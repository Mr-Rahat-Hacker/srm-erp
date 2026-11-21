package com.srm.erp.rfq.api;

import java.time.Instant;
import java.util.UUID;

public record RfqSummaryResponse(
        UUID rfqId,
        String rfqNumber,
        String title,
        String status,
        String currencyCode,
        Instant submissionDeadline,
        int lineCount,
        int invitationCount,
        Instant createdAt
) {}
