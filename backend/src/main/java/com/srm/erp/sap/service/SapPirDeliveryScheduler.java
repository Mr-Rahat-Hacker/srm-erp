package com.srm.erp.sap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "srm.sap.pir.enabled", havingValue = "true")
public class SapPirDeliveryScheduler {
    private final JdbcTemplate jdbc;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final String gatewayUrl;
    private final String bearerToken;

    public SapPirDeliveryScheduler(JdbcTemplate jdbc, RestClient.Builder restClientBuilder,
                                   ObjectMapper objectMapper,
                                   @Value("${srm.sap.pir.gateway-url:}") String gatewayUrl,
                                   @Value("${srm.sap.pir.bearer-token:}") String bearerToken) {
        this.jdbc = jdbc;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.gatewayUrl = gatewayUrl;
        this.bearerToken = bearerToken;
    }

    @Scheduled(fixedDelayString = "${srm.sap.pir.poll-interval-ms:30000}")
    public void deliverQueued() {
        if (gatewayUrl == null || gatewayUrl.isBlank() || bearerToken == null || bearerToken.isBlank()) return;
        for (SapJob job : claimQueued()) {
            try {
                JsonNode payload = objectMapper.readTree(job.payload());
                var response = restClientBuilder.build().post()
                        .uri(gatewayUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bearerToken)
                        .header("Idempotency-Key", job.requestKey().toString())
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                String externalReference = response.getHeaders().getFirst("X-Integration-Request-Id");
                jdbc.update("""
                        UPDATE srm.sap_integration_event
                        SET status = 'SUCCEEDED', external_reference = ?,
                            response_payload = jsonb_build_object('httpStatus', ?, 'acceptedAt', now()),
                            sanitized_error = NULL, updated_at = now()
                        WHERE integration_event_id = ? AND status = 'SENDING'
                        """, externalReference, response.getStatusCode().value(), job.id());
            } catch (Exception failure) {
                jdbc.update("""
                        UPDATE srm.sap_integration_event
                        SET status = 'FAILED', sanitized_error = ?, updated_at = now()
                        WHERE integration_event_id = ? AND status = 'SENDING'
                        """, failure.getClass().getSimpleName(), job.id());
            }
        }
    }

    private List<SapJob> claimQueued() {
        jdbc.update("""
                UPDATE srm.sap_integration_event
                SET status = 'QUEUED', updated_at = now()
                WHERE status = 'SENDING' AND updated_at < now() - interval '15 minutes'
                """);
        return jdbc.query("""
                UPDATE srm.sap_integration_event e
                SET status = 'SENDING', attempt_count = e.attempt_count + 1, updated_at = now()
                FROM (
                    SELECT integration_event_id
                    FROM srm.sap_integration_event
                    WHERE operation_type = 'PIR_UPSERT' AND status = 'QUEUED'
                    ORDER BY created_at
                    LIMIT 10
                    FOR UPDATE SKIP LOCKED
                ) queued
                WHERE e.integration_event_id = queued.integration_event_id
                RETURNING e.integration_event_id, e.request_key, e.request_payload::text
                """, (rs, row) -> map(rs));
    }

    private SapJob map(ResultSet rs) throws SQLException {
        return new SapJob(rs.getObject("integration_event_id", UUID.class),
                rs.getObject("request_key", UUID.class), rs.getString("request_payload"));
    }

    private record SapJob(UUID id, UUID requestKey, String payload) {}
}
