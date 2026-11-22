package com.srm.erp.security;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AdminUserService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminUser> findAll() {
        return jdbc.query("""
                SELECT u.user_id, u.email, u.full_name, u.account_status,
                       COALESCE(string_agg(r.role_code, ',' ORDER BY r.role_code), '') AS roles
                FROM srm.app_user u
                LEFT JOIN srm.user_role ur ON ur.user_id = u.user_id
                LEFT JOIN srm.role r ON r.role_id = ur.role_id
                GROUP BY u.user_id
                ORDER BY lower(u.email)
                """, (rs, row) -> new AdminUser(rs.getObject("user_id", UUID.class),
                rs.getString("email"), rs.getString("full_name"), rs.getString("account_status"),
                rs.getString("roles").isBlank() ? List.of() : List.of(rs.getString("roles").split(","))));
    }

    @Transactional
    public AdminUser create(AdminUserController.CreateUserRequest request, UUID actorId) {
        String password = request.password();
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM srm.app_user WHERE lower(email) = ?)",
                Boolean.class, email);
        if (Boolean.TRUE.equals(exists)) {
            throw new ResponseStatusException(CONFLICT, "An account with this email already exists");
        }

        List<String> roles = request.roles().stream().map(role -> role.trim().toUpperCase(Locale.ROOT))
                .distinct().sorted().toList();
        if (roles.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "At least one role is required");

        UUID userId;
        try {
            userId = jdbc.queryForObject("""
                    INSERT INTO srm.app_user (email, password_hash, full_name, account_status)
                    VALUES (?, ?, ?, 'ACTIVE') RETURNING user_id
                    """, UUID.class, email, passwordEncoder.encode(password), request.fullName().trim());
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(CONFLICT, "An account with this email already exists");
        }

        for (String role : roles) {
            List<UUID> roleIds = jdbc.query("SELECT role_id FROM srm.role WHERE role_code = ?",
                    (rs, row) -> rs.getObject(1, UUID.class), role);
            if (roleIds.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "Unknown role: " + role);
            jdbc.update("INSERT INTO srm.user_role (user_id, role_id, assigned_by) VALUES (?, ?, ?)",
                    userId, roleIds.getFirst(), actorId);
        }

        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id,
                     outcome, changed_fields)
                VALUES (?, 'SUPER_ADMIN', 'USER_CREATED', 'USER', ?, 'SUCCESS',
                        jsonb_build_object('email', ?, 'roles', ?))
                """, actorId, userId, email, String.join(",", roles));

        return new AdminUser(userId, email, request.fullName().trim(), "ACTIVE", roles);
    }

    @Transactional
    public AdminUser updateStatus(UUID userId, String accountStatus, UUID actorId) {
        String next = accountStatus == null ? "" : accountStatus.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVE", "SUSPENDED", "DEACTIVATED").contains(next)) {
            throw new ResponseStatusException(BAD_REQUEST, "Status must be ACTIVE, SUSPENDED or DEACTIVATED");
        }
        if (userId.equals(actorId) && !"ACTIVE".equals(next)) {
            throw new ResponseStatusException(CONFLICT, "You cannot suspend or deactivate your own account");
        }

        List<String> currentStatuses = jdbc.query(
                "SELECT account_status FROM srm.app_user WHERE user_id = ?",
                (rs, row) -> rs.getString(1), userId);
        if (currentStatuses.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "User not found");
        String previous = currentStatuses.getFirst();
        if (previous.equals(next)) return findById(userId);

        jdbc.update("UPDATE srm.app_user SET account_status = ?, updated_at = now() WHERE user_id = ?",
                next, userId);
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id,
                     outcome, changed_fields)
                VALUES (?, 'SUPER_ADMIN', 'USER_STATUS_CHANGED', 'USER', ?, 'SUCCESS',
                        jsonb_build_object('account_status', jsonb_build_object('old', ?, 'new', ?)))
                """, actorId, userId, previous, next);
        return findById(userId);
    }

    private AdminUser findById(UUID userId) {
        List<AdminUser> users = jdbc.query("""
                SELECT u.user_id, u.email, u.full_name, u.account_status,
                       COALESCE(string_agg(r.role_code, ',' ORDER BY r.role_code), '') AS roles
                FROM srm.app_user u
                LEFT JOIN srm.user_role ur ON ur.user_id = u.user_id
                LEFT JOIN srm.role r ON r.role_id = ur.role_id
                WHERE u.user_id = ?
                GROUP BY u.user_id
                """, (rs, row) -> new AdminUser(rs.getObject("user_id", UUID.class),
                rs.getString("email"), rs.getString("full_name"), rs.getString("account_status"),
                rs.getString("roles").isBlank() ? List.of() : List.of(rs.getString("roles").split(","))), userId);
        if (users.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "User not found");
        return users.getFirst();
    }

    public record AdminUser(UUID userId, String email, String fullName, String accountStatus, List<String> roles) {}
}
