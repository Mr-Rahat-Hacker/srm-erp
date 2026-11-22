package com.srm.erp.supplier.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierLifecycleServiceTest {
    private static final String STATUS_QUERY =
            "SELECT lifecycle_status FROM srm.supplier_organization WHERE supplier_id = ?";
    private static final UUID SUPPLIER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Mock
    private JdbcTemplate jdbc;

    private SupplierLifecycleService service;
    private final List<Object[]> updateArguments = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new SupplierLifecycleService(jdbc);
    }

    @Test
    void submitMovesDraftToSubmittedAndAuditsTheAuthorizedAdminActor() {
        givenStatus("DRAFT");
        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            updateArguments.add(Arrays.copyOfRange(arguments, 1, arguments.length));
            return 1;
        }).when(jdbc).update(anyString(), any(Object[].class));

        var result = service.submit(SUPPLIER_ID, ACTOR_ID);

        assertEquals("DRAFT", result.previousStatus());
        assertEquals("SUBMITTED", result.currentStatus());
        assertEquals("SUPPLIER_SUBMITTED", result.action());
        assertEquals(4, updateArguments.size());
        assertEquals("SRM_ADMIN", updateArguments.get(2)[1]);
        assertEquals(SUPPLIER_ID, updateArguments.get(3)[0]);
    }

    @Test
    void supplierUserCanSubmitOwnDraftAndAuditCorrectActorType() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(SUPPLIER_ID), eq(ACTOR_ID)))
                .thenReturn(true);
        givenStatus("DRAFT");
        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            updateArguments.add(Arrays.copyOfRange(arguments, 1, arguments.length));
            return 1;
        }).when(jdbc).update(anyString(), any(Object[].class));

        var result = service.submitForSupplierUser(SUPPLIER_ID, ACTOR_ID);

        assertEquals("SUBMITTED", result.currentStatus());
        assertEquals("SUPPLIER_SUBMITTED", result.action());
        assertEquals("SUPPLIER_USER", updateArguments.get(2)[1]);
        verify(jdbc).queryForObject(anyString(), eq(Boolean.class), eq(SUPPLIER_ID), eq(ACTOR_ID));
    }

    @Test
    void supplierUserCannotSubmitAnotherOrganizationsProfile() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(SUPPLIER_ID), eq(ACTOR_ID)))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.submitForSupplierUser(SUPPLIER_ID, ACTOR_ID));

        assertEquals(404, exception.getStatusCode().value());
        verify(jdbc).queryForObject(anyString(), eq(Boolean.class), eq(SUPPLIER_ID), eq(ACTOR_ID));
        verify(jdbc, never()).query(eq(STATUS_QUERY), ArgumentMatchers.<RowMapper<String>>any(), eq(SUPPLIER_ID));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void startReviewRejectsSuppliersThatWereNotSubmitted() {
        givenStatus("DRAFT");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.startReview(SUPPLIER_ID, ACTOR_ID));

        assertEquals(409, exception.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void rejectionRequiresAReasonBeforeAnyReviewWrite() {
        givenStatus("UNDER_REVIEW");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.decide(SUPPLIER_ID, ACTOR_ID, "REJECT", "  "));

        assertEquals(400, exception.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void missingSupplierReturnsNotFound() {
        when(jdbc.query(eq(STATUS_QUERY), ArgumentMatchers.<RowMapper<String>>any(), eq(SUPPLIER_ID)))
                .thenReturn(List.of());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.submit(SUPPLIER_ID, ACTOR_ID));

        assertEquals(404, exception.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    private void givenStatus(String status) {
        when(jdbc.query(eq(STATUS_QUERY), ArgumentMatchers.<RowMapper<String>>any(), eq(SUPPLIER_ID)))
                .thenReturn(List.of(status));
    }
}
