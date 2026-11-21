package com.srm.erp.notification.api;

import com.srm.erp.notification.service.NotificationService;
import com.srm.erp.platform.response.ApiResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public ApiResponse<List<NotificationRecord>> inbox() {
        return ApiResponse.success("Notifications retrieved", notifications.inbox(actorId()));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationRecord> markRead(@PathVariable UUID notificationId) {
        return ApiResponse.success("Notification marked as read",
                notifications.markRead(notificationId, actorId()));
    }

    private UUID actorId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
