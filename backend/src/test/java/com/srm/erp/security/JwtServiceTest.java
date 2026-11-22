package com.srm.erp.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000123");
    private static final String SECRET = "test-signing-key-with-at-least-32-bytes";

    @Test
    void signsAndReadsTokenWithConfiguredKey() {
        JwtService service = new JwtService(SECRET, 300, new MockEnvironment());
        String token = service.generateToken(USER_ID, "buyer@example.test", List.of("PROCUREMENT_BUYER"));

        assertEquals(USER_ID, service.extractUserId(token));
        assertEquals(List.of("PROCUREMENT_BUYER"), service.extractRoles(token));
    }

    @Test
    void productionProfileRequiresConfiguredSigningKey() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new JwtService("", 300, environment));

        assertEquals("SRM_JWT_SECRET must be configured for production", error.getMessage());
    }

    @Test
    void developmentFallbackIsRandomAndDoesNotReuseSigningKey() {
        JwtService first = new JwtService("", 300, new MockEnvironment());
        JwtService second = new JwtService("", 300, new MockEnvironment());
        String token = first.generateToken(USER_ID, "dev@example.test", List.of());

        assertEquals(USER_ID, first.extractUserId(token));
        assertThrows(JwtException.class, () -> second.extractUserId(token));
    }

    @Test
    void rejectsConfiguredKeysShorterThanThirtyTwoBytes() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("short", 300, new MockEnvironment()));
    }
}
