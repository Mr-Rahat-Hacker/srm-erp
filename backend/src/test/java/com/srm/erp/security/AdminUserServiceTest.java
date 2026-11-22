package com.srm.erp.security;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        verify(jdbc, never()).query(anyString(), any(org.springframework.jdbc.core.RowMapper.class),
                any(Object[].class));
    }
}
