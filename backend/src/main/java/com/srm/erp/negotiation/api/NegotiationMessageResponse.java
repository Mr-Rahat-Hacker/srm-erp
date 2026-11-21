package com.srm.erp.negotiation.api;

import java.time.Instant;
import java.util.UUID;

public record NegotiationMessageResponse(
        UUID messageId,
        UUID senderUserId,
        String senderName,
        String senderRole,
        String messageType,
        String messageText,
        UUID quotationVersionId,
        Integer quotationVersionNumber,
        Instant createdAt
) {}
