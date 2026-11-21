package com.srm.erp.quotation.api;

import java.time.Instant;
import java.util.UUID;

public record SupplierRfqSummary(
        UUID rfqId,
        String rfqNumber,
        String title,
        String currencyCode,
        Instant submissionDeadline,
        String invitationStatus,
        String quotationStatus,
        Integer latestVersion
) {}
