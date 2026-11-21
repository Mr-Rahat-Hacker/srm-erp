package com.srm.erp.approval.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AwardResponse(UUID awardId, UUID rfqId, String rfqNumber, UUID supplierId,
                            String supplierName, UUID quotationVersionId, Integer versionNumber,
                            UUID approvalRequestId, BigDecimal approvedValue, String currencyCode,
                            String status, Instant awardedAt) {}
