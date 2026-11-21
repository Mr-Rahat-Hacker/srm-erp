package com.srm.erp.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * One-time, environment-gated bootstrap for the first platform administrator.
 * No HTTP endpoint is exposed and no credentials are stored in source control.
 */
@Component
public class FirstAdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(FirstAdminBootstrap.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    @Value("${srm.bootstrap.enabled:false}")
    private boolean enabled;

    @Value("${srm.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${srm.bootstrap.admin.full-name:}")
    private String adminFullName;

    @Value("${srm.bootstrap.admin.password:}")
    private String adminPassword;

    public FirstAdminBootstrap(JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
                               TransactionTemplate transactionTemplate) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;

        String email = adminEmail == null ? "" : adminEmail.trim().toLowerCase(Locale.ROOT);
        String fullName = adminFullName == null ? "" : adminFullName.trim();
        String password = adminPassword == null ? "" : adminPassword;

        if (email.isBlank() || fullName.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Admin bootstrap enabled but email, full name, or password is missing.");
        }
        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 12 || passwordBytes > 72) {
            throw new IllegalStateException("Bootstrap password must be between 12 and 72 UTF-8 bytes.");
        }

        Boolean created = transactionTemplate.execute(status -> {
            // Serialize concurrent first-start attempts across application instances.
            jdbc.execute("SELECT pg_advisory_xact_lock(74192026)");
            Integer userCount = jdbc.queryForObject("SELECT count(*) FROM srm.app_user", Integer.class);
            if (userCount != null && userCount > 0) return false;

            UUID userId = UUID.randomUUID();
            String passwordHash = passwordEncoder.encode(password);
            jdbc.update("""
                    INSERT INTO srm.app_user
                        (user_id, email, password_hash, full_name, account_status)
                    VALUES (?, ?, ?, ?, 'ACTIVE')
                    """, userId, email, passwordHash, fullName);

            int assigned = jdbc.update("""
                    INSERT INTO srm.user_role (user_id, role_id, assigned_by)
                    SELECT ?, role_id, ? FROM srm.role WHERE role_code = 'SUPER_ADMIN'
                    """, userId, userId);
            if (assigned != 1) {
                throw new IllegalStateException("SUPER_ADMIN role is missing; verify Flyway role seed migration.");
            }

            jdbc.update("""
                    INSERT INTO srm.audit_event
                        (actor_user_id, actor_type, effective_roles, action_code,
                         entity_type, entity_id, outcome, reason)
                    VALUES (?, 'SYSTEM', ARRAY['SUPER_ADMIN'], 'BOOTSTRAP_SUPER_ADMIN',
                            'APP_USER', ?, 'SUCCESS', 'First administrator provisioned through environment-gated bootstrap')
                    """, userId, userId);
            return true;
        });

        if (Boolean.TRUE.equals(created)) {
            log.warn("First SRM SUPER_ADMIN account provisioned for {}. Remove bootstrap environment variables and restart the application.", email);
        } else {
            log.warn("SRM bootstrap skipped: an application user already exists. No account or role was changed.");
        }
    }
}
