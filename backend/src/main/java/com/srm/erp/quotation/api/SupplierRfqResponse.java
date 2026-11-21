package com.srm.erp.quotation.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SupplierRfqResponse(
        UUID rfqId,
        String rfqNumber,
        String title,
        String description,
        String currencyCode,
        Instant submissionDeadline,
        LocalDate requiredDeliveryDate,
        String paymentTerms,
        String incoterms,
        String invitationStatus,
        List<SupplierRfqLine> lines,
        List<QuotationVersionResponse> quotationVersions
) {}
