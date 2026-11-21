package com.srm.erp.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "srm.notifications.email.enabled", havingValue = "true")
public class EmailDeliveryScheduler {
    private final JdbcTemplate jdbc;
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String publicBaseUrl;
    private final int maxAttempts;

    public EmailDeliveryScheduler(JdbcTemplate jdbc, JavaMailSender mailSender,
                                  @Value("${srm.notifications.email.from:no-reply@srm.local}") String fromAddress,
                                  @Value("${srm.notifications.email.public-base-url:http://localhost:4200}") String publicBaseUrl,
                                  @Value("${srm.notifications.email.max-attempts:5}") int maxAttempts) {
        this.jdbc = jdbc;
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.publicBaseUrl = publicBaseUrl;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Scheduled(fixedDelayString = "${srm.notifications.email.poll-interval-ms:30000}")
    public void deliverQueued() {
        List<EmailJob> jobs = claimQueued();
        for (EmailJob job : jobs) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(job.email());
                message.setSubject(job.title());
                String link = job.targetPath() == null ? "" : System.lineSeparator()
                        + System.lineSeparator() + "Open SRM: "
                        + publicBaseUrl.replaceAll("/+$", "") + job.targetPath();
                message.setText(job.body() + link);
                mailSender.send(message);
                jdbc.update("""
                        UPDATE srm.notification SET delivery_status = 'SENT', sent_at = now(), last_error = NULL
                        WHERE notification_id = ? AND delivery_status = 'SENDING'
                        """, job.id());
            } catch (Exception failure) {
                String status = job.attempt() >= maxAttempts ? "FAILED" : "QUEUED";
                String reason = failure.getClass().getSimpleName();
                jdbc.update("""
                        UPDATE srm.notification SET delivery_status = ?, last_error = ?
                        WHERE notification_id = ? AND delivery_status = 'SENDING'
                        """, status, reason, job.id());
            }
        }
    }

    private List<EmailJob> claimQueued() {
        return jdbc.query("""
                UPDATE srm.notification n
                SET delivery_status = 'SENDING', retry_count = n.retry_count + 1
                FROM (
                    SELECT queued.notification_id, u.email, queued.title, queued.body,
                           queued.target_path
                    FROM srm.notification queued
                    JOIN srm.app_user u ON u.user_id = queued.recipient_user_id
                    WHERE queued.channel = 'EMAIL' AND queued.delivery_status = 'QUEUED'
                      AND u.email IS NOT NULL
                    ORDER BY queued.created_at
                    LIMIT 20
                    FOR UPDATE OF queued SKIP LOCKED
                ) queued
                WHERE n.notification_id = queued.notification_id
                RETURNING n.notification_id, queued.email, queued.title, queued.body,
                          queued.target_path, n.retry_count
                """, (rs, row) -> map(rs));
    }

    private EmailJob map(ResultSet rs) throws SQLException {
        return new EmailJob(rs.getObject("notification_id", UUID.class), rs.getString("email"),
                rs.getString("title"), rs.getString("body"), rs.getString("target_path"),
                rs.getInt("retry_count"));
    }

    private record EmailJob(UUID id, String email, String title, String body, String targetPath, int attempt) {}
}
