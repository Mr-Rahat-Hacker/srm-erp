package com.srm.erp.security;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {
    @Test
    void preventsSuperAdminFromDeactivatingTheirOwnAccount() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AdminUserService service = new AdminUserService(jdbc, mock(PasswordEncoder.class));
        UUID actorId = UUID.randomUUID();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateStatus(actorId, "DEACTIVATED", actorId));

        assertEquals(409, error.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void rejectsUnsupportedAccountStatusBeforeDatabaseAccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AdminUserService service = new AdminUserService(jdbc, mock(PasswordEncoder.class));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateStatus(UUID.randomUUID(), "PENDING", UUID.randomUUID()));

        assertEquals(400, error.getStatusCode().value());
        verify(jdbc, never()).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void rejectsSupplierRoleChangesBeforeDatabaseAccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AdminUserService service = new AdminUserService(jdbc, mock(PasswordEncoder.class));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateRoles(UUID.randomUUID(), List.of("SUPPLIER_USER"), UUID.randomUUID()));

        assertEquals(400, error.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void refusesToRemoveTheLastActiveSuperAdmin() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AdminUserService service = new AdminUserService(jdbc, mock(PasswordEncoder.class));
        UUID userId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        AdminUserService.AdminUser target = new AdminUserService.AdminUser(
                userId, "root@example.com", "Platform Owner", "ACTIVE", List.of("SUPER_ADMIN"));
        doAnswer(invocation -> List.of(target))
                .when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
        when(jdbc.queryForObject(contains("count(DISTINCT u.user_id)"), eq(Long.class), eq(userId)))
                .thenReturn(0L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateRoles(userId, List.of("SRM_ADMIN"), actorId));

        assertEquals(409, error.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
