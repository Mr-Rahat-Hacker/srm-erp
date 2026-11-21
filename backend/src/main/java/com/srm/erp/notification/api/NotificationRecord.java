package com.srm.erp.notification.api;

import java.time.Instant;
import java.util.UUID;

public record NotificationRecord(UUID notificationId, String eventType, String title, String body,
                                 String targetPath, String deliveryStatus, Instant createdAt,
                                 Instant sentAt, Instant readAt, int retryCount, String lastError) {}
