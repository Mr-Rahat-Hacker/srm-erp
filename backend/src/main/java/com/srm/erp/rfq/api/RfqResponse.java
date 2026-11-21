package com.srm.erp.rfq.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RfqResponse(
        UUID rfqId,
        String rfqNumber,
        String title,
        String description,
        String projectCode,
        String department,
        String plantCode,
        String purchasingGroup,
        String currencyCode,
        Instant submissionDeadline,
        LocalDate requiredDeliveryDate,
        String paymentTerms,
        String incoterms,
        String status,
        Instant createdAt,
        List<RfqLineResponse> lines,
        List<RfqInvitationResponse> invitations
) {}
