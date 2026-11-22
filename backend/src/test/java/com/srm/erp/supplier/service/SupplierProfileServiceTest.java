package com.srm.erp.supplier.service;

import com.srm.erp.supplier.api.SupplierProfileUpdateRequest;
import com.srm.erp.supplier.api.SupplierResponse;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierProfileServiceTest {
    private static final UUID SUPPLIER_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");

    @Test
    void doesNotReadOrWriteProfileFieldsForAnotherOrganizationsSupplier() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SupplierService suppliers = mock(SupplierService.class);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(SUPPLIER_ID), eq(USER_ID)))
                .thenReturn(List.of());
        SupplierProfileService service = new SupplierProfileService(jdbc, suppliers);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateForUser(SUPPLIER_ID, USER_ID, request("New Name")));

        assertEquals(404, error.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(suppliers, never()).findByIdPublic(SUPPLIER_ID);
    }

    @Test
    void preventsEditingProfileWhileItIsUnderReview() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SupplierService suppliers = mock(SupplierService.class);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(SUPPLIER_ID), eq(USER_ID)))
                .thenReturn(List.of("UNDER_REVIEW"));
        SupplierProfileService service = new SupplierProfileService(jdbc, suppliers);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateForUser(SUPPLIER_ID, USER_ID, request("New Name")));

        assertEquals(409, error.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(suppliers, never()).findByIdPublic(SUPPLIER_ID);
    }

    @Test
    void updatesOwnedDraftAndRecordsSupplierActorInAudit() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SupplierService suppliers = mock(SupplierService.class);
        SupplierResponse response = new SupplierResponse(
                SUPPLIER_ID, "SUP-401", "New Name", null, null, null, "DRAFT",
                "Supplier Owner", "owner@example.com", null, Instant.parse("2026-11-22T00:00:00Z"));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(SUPPLIER_ID), eq(USER_ID)))
                .thenReturn(List.of("DRAFT"));
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(SUPPLIER_ID))).thenReturn(false);
        when(suppliers.findByIdPublic(SUPPLIER_ID)).thenReturn(response);
        SupplierProfileService service = new SupplierProfileService(jdbc, suppliers);

        SupplierResponse result = service.updateForUser(SUPPLIER_ID, USER_ID, request("New Name"));

        assertSame(response, result);
        verify(jdbc).update(
                org.mockito.ArgumentMatchers.contains("UPDATE srm.supplier_organization"),
                any(Object[].class));
        verify(jdbc).update(
                org.mockito.ArgumentMatchers.contains("'SUPPLIER_PROFILE_UPDATED'"),
                eq(USER_ID), eq(SUPPLIER_ID), eq(SUPPLIER_ID), eq("legal_name"));
        verify(suppliers).findByIdPublic(SUPPLIER_ID);
    }

    private SupplierProfileUpdateRequest request(String legalName) {
        return new SupplierProfileUpdateRequest(
                legalName, null, null, null, null, null, null,
                null, null, null, null, null);
    }
}
