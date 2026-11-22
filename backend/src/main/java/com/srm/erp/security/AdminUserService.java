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
                """, (rs, row) -> mapUser(rs.getObject("user_id", UUID.class), rs.getString("email"),
                rs.getString("full_name"), rs.getString("account_status"), rs.getString("roles")));
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

        List<String> roles = normalizeRoles(request.roles());
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
            UUID roleId = roleId(role);
            jdbc.update("INSERT INTO srm.user_role (user_id, role_id, assigned_by) VALUES (?, ?, ?)",
                    userId, roleId, actorId);
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
    public AdminUser updateRoles(UUID userId, List<String> requestedRoles, UUID actorId) {
        List<String> roles = normalizeRoles(requestedRoles);
        if (roles.contains("SUPPLIER_USER")) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Supplier roles are managed through supplier membership, not internal user administration");
        }

        AdminUser target = findById(userId);
        if (target.roles().contains("SUPPLIER_USER")) {
            throw new ResponseStatusException(CONFLICT,
                    "Supplier accounts cannot be changed through internal user administration");
        }
        if (userId.equals(actorId) && !roles.contains("SUPER_ADMIN")) {
            throw new ResponseStatusException(CONFLICT, "You cannot remove your own Super Admin role");
        }
        if ("ACTIVE".equals(target.accountStatus()) && target.roles().contains("SUPER_ADMIN")
                && !roles.contains("SUPER_ADMIN")) {
            ensureAnotherActiveSuperAdmin(userId);
        }
        if (target.roles().equals(roles)) return target;

        List<UUID> roleIds = roles.stream().map(this::roleId).toList();
        jdbc.update("DELETE FROM srm.user_role WHERE user_id = ?", userId);
        for (UUID roleId : roleIds) {
            jdbc.update("INSERT INTO srm.user_role (user_id, role_id, assigned_by) VALUES (?, ?, ?)",
                    userId, roleId, actorId);
        }
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id,
                     outcome, changed_fields)
                VALUES (?, 'SUPER_ADMIN', 'USER_ROLES_CHANGED', 'USER', ?, 'SUCCESS',
                        jsonb_build_object('roles', jsonb_build_object('old', ?, 'new', ?)))
                """, actorId, userId, String.join(",", target.roles()), String.join(",", roles));
        return findById(userId);
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

        AdminUser target = findById(userId);
        String previous = target.accountStatus();
        if (previous.equals(next)) return target;
        if ("ACTIVE".equals(previous) && !"ACTIVE".equals(next) && target.roles().contains("SUPER_ADMIN")) {
            ensureAnotherActiveSuperAdmin(userId);
        }

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

    private UUID roleId(String role) {
        List<UUID> roleIds = jdbc.query("SELECT role_id FROM srm.role WHERE role_code = ?",
                (rs, row) -> rs.getObject(1, UUID.class), role);
        if (roleIds.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "Unknown role: " + role);
        return roleIds.getFirst();
    }

    private List<String> normalizeRoles(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one role is required");
        }
        List<String> roles = requested.stream().map(role -> role.trim().toUpperCase(Locale.ROOT))
                .distinct().sorted().toList();
        if (roles.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "At least one role is required");
        return roles;
    }

    private void ensureAnotherActiveSuperAdmin(UUID excludedUserId) {
        Long count = jdbc.queryForObject("""
                SELECT count(DISTINCT u.user_id)
                FROM srm.app_user u
                JOIN srm.user_role ur ON ur.user_id = u.user_id
                JOIN srm.role r ON r.role_id = ur.role_id
                WHERE u.account_status = 'ACTIVE' AND u.user_id <> ? AND r.role_code = 'SUPER_ADMIN'
                """, Long.class, excludedUserId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(CONFLICT,
                    "At least one other active Super Admin must remain");
        }
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
                """, (rs, row) -> mapUser(rs.getObject("user_id", UUID.class), rs.getString("email"),
                rs.getString("full_name"), rs.getString("account_status"), rs.getString("roles")), userId);
        if (users.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "User not found");
        return users.getFirst();
    }

    private AdminUser mapUser(UUID userId, String email, String fullName, String accountStatus, String roleCsv) {
        List<String> roles = roleCsv == null || roleCsv.isBlank()
                ? List.of() : List.of(roleCsv.split(","));
        return new AdminUser(userId, email, fullName, accountStatus, roles);
    }

    public record AdminUser(UUID userId, String email, String fullName, String accountStatus, List<String> roles) {}
}
