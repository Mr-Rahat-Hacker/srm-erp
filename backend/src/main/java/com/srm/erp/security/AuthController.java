package com.srm.erp.security;

import com.srm.erp.platform.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final JdbcTemplate jdbc;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(JdbcTemplate jdbc,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var users = jdbc.query("""
                SELECT user_id, email, password_hash, full_name, account_status
                FROM srm.app_user WHERE lower(email) = lower(?)
                """, (rs, row) -> new UserRow(
                rs.getObject("user_id", UUID.class), rs.getString("email"),
                rs.getString("password_hash"), rs.getString("full_name"),
                rs.getString("account_status")), request.email().trim());

        if (users.isEmpty()) throw new ResponseStatusException(UNAUTHORIZED, "Invalid email or password");
        UserRow user = users.getFirst();
        if (!"ACTIVE".equals(user.status()) || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid email or password");
        }

        List<String> roles = jdbc.query("""
                SELECT r.role_code FROM srm.user_role ur
                JOIN srm.role r ON r.role_id = ur.role_id
                WHERE ur.user_id = ? ORDER BY r.role_code
                """, (rs, row) -> rs.getString(1), user.id());

        if (roles.isEmpty()) throw new ResponseStatusException(UNAUTHORIZED, "No active role assigned to this account");
        String token = jwtService.generateToken(user.id(), user.email(), roles);
        return ApiResponse.success("Login successful",
                new LoginResponse(token, "Bearer", jwtService.expirationSeconds(),
                        user.id(), user.email(), user.fullName(), roles));
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresIn,
                                UUID userId, String email, String fullName, List<String> roles) {}
    record UserRow(UUID id, String email, String passwordHash, String fullName, String status) {}
}
