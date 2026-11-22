package com.srm.erp.security;

import com.srm.erp.platform.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void loginReturnsConfiguredTokenExpiry() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000321");
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FROM srm.app_user")) {
                return List.of(new AuthController.UserRow(
                        userId, "buyer@example.com", "encoded-password", "Buyer User", "ACTIVE"));
            }
            return List.of("PROCUREMENT_BUYER");
        }).when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
        when(encoder.matches("correct password", "encoded-password")).thenReturn(true);

        JwtService jwt = new JwtService(
                "this-is-a-long-test-secret-with-over-32-bytes", 240, new MockEnvironment());
        AuthController controller = new AuthController(jdbc, encoder, jwt);

        ApiResponse<AuthController.LoginResponse> response =
                controller.login(new AuthController.LoginRequest("buyer@example.com", "correct password"));

        assertEquals(240, response.data().expiresIn());
        assertEquals(userId, response.data().userId());
    }
}
