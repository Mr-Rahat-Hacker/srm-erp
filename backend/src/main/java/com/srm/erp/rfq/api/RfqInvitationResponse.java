package com.srm.erp.rfq.api;

import java.time.Instant;
import java.util.UUID;

public record RfqInvitationResponse(
        UUID supplierId,
        String supplierNumber,
        String legalName,
        String invitationStatus,
        Instant invitedAt
) {}
