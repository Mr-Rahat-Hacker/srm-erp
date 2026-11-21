package com.srm.erp.notification.service;

import com.srm.erp.notification.api.NotificationRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NotificationService {
    private final JdbcTemplate jdbc;

    public NotificationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<NotificationRecord> inbox(UUID recipientId) {
        return jdbc.query("""
                SELECT notification_id, event_type, title, body, target_path, delivery_status,
                       created_at, sent_at, read_at, retry_count, last_error
                FROM srm.notification
                WHERE recipient_user_id = ? AND channel = 'IN_APP'
                ORDER BY created_at DESC
                LIMIT 100
                """, (rs, row) -> map(rs), recipientId);
    }

    public NotificationRecord markRead(UUID notificationId, UUID recipientId) {
        return jdbc.query("""
                UPDATE srm.notification
                SET read_at = COALESCE(read_at, now())
                WHERE notification_id = ? AND recipient_user_id = ? AND channel = 'IN_APP'
                RETURNING notification_id, event_type, title, body, target_path, delivery_status,
                          created_at, sent_at, read_at, retry_count, last_error
                """, rs -> rs.next() ? map(rs) : null, notificationId, recipientId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notification not found"));
    }

    private NotificationRecord map(ResultSet rs) throws SQLException {
        return new NotificationRecord(rs.getObject("notification_id", UUID.class),
                rs.getString("event_type"), rs.getString("title"), rs.getString("body"),
                rs.getString("target_path"), rs.getString("delivery_status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toInstant(),
                rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant(),
                rs.getInt("retry_count"), rs.getString("last_error"));
    }
}
