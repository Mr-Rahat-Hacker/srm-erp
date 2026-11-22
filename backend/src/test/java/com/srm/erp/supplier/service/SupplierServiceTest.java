package com.srm.erp.supplier.service;

import com.srm.erp.supplier.api.SupplierRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class SupplierServiceTest {
    @Test
    void rejectsShortPasswordBeforeDatabaseAccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        SupplierService service = new SupplierService(jdbc, encoder);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.registerSelf(request("short")));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc, encoder);
    }

    @Test
    void rejectsPasswordOverBcryptByteLimitBeforeDatabaseAccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        SupplierService service = new SupplierService(jdbc, encoder);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.registerSelf(request("x".repeat(73))));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc, encoder);
    }

    @Test
    void rejectsExistingEmailWithoutCreatingSupplierOrAccount() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        String email = "owner@example.com";
        doReturn(true).when(jdbc).queryForObject(
                contains("lower(email)"), eq(Boolean.class), eq(email));
        SupplierService service = new SupplierService(jdbc, encoder);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.registerSelf(request("correct horse battery staple")));

        assertEquals(409, error.getStatusCode().value());
        verify(jdbc).queryForObject(contains("lower(email)"), eq(Boolean.class), eq(email));
        verifyNoMoreInteractions(jdbc);
        verifyNoInteractions(encoder);
    }

    private SupplierRegistrationRequest request(String password) {
        return new SupplierRegistrationRequest(
                "Example Components", null, null, null, "Account Owner", "owner@example.com",
                null, null, null, null, null, "IN", password);
    }
}
