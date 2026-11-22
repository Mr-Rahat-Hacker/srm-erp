package com.srm.erp.approval.service;

import com.srm.erp.approval.api.AwardApprovalPolicyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AwardApprovalPolicyServiceTest {
    private JdbcTemplate jdbc;
    private AwardApprovalPolicyService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new AwardApprovalPolicyService(jdbc, mock(AwardApprovalService.class));
    }

    @Test
    void rejectsMaximumThatDoesNotExceedMinimumBeforeDatabaseAccess() {
        var request = request(new BigDecimal("100.00"), new BigDecimal("100.00"), List.of(UUID.randomUUID()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(UUID.randomUUID(), request));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsNegativeMinimumBeforeDatabaseAccess() {
        var request = request(new BigDecimal("-0.01"), null, List.of(UUID.randomUUID()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(UUID.randomUUID(), request));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsAmountsWithMoreThanTwoFractionalDigitsBeforeDatabaseAccess() {
        var request = request(new BigDecimal("1.001"), null, List.of(UUID.randomUUID()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(UUID.randomUUID(), request));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsBlankReasonBeforeDatabaseAccess() {
        var request = new AwardApprovalPolicyRequest("LOW", "Low value", "USD",
                BigDecimal.ZERO, null, List.of(UUID.randomUUID()), " ");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(UUID.randomUUID(), request));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    private AwardApprovalPolicyRequest request(BigDecimal minimum, BigDecimal maximum, List<UUID> approvers) {
        return new AwardApprovalPolicyRequest("LOW", "Low value", "USD",
                minimum, maximum, approvers, "Initial approval policy");
    }
}
