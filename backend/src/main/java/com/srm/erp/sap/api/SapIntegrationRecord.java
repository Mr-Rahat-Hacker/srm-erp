package com.srm.erp.sap.api;

import java.time.Instant;
import java.util.UUID;

public record SapIntegrationRecord(UUID integrationEventId, UUID requestKey, UUID awardId,
                                   String operationType, String status, String externalReference,
                                   String sanitizedError, int attemptCount, Instant createdAt,
                                   Instant updatedAt) {}
