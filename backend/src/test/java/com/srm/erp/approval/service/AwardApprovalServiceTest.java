package com.srm.erp.approval.service;

import com.srm.erp.approval.api.ApprovalDecisionRequest;
import com.srm.erp.approval.api.AwardApprovalRequest;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AwardApprovalServiceTest {
    private JdbcTemplate jdbc;
    private AwardApprovalService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new AwardApprovalService(jdbc);
    }

    @Test
    void rejectsDuplicateApproversInSequentialRouteBeforeDatabaseAccess() {
        UUID approver = UUID.randomUUID();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requestApproval(UUID.randomUUID(), UUID.randomUUID(),
                        new AwardApprovalRequest(UUID.randomUUID(), null, List.of(approver, approver))));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsNullStageInSequentialRouteBeforeDatabaseAccess() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requestApproval(UUID.randomUUID(), UUID.randomUUID(),
                        new AwardApprovalRequest(UUID.randomUUID(), null,
                                java.util.Arrays.asList(UUID.randomUUID(), null))));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsMoreThanTenApprovalStagesBeforeDatabaseAccess() {
        List<UUID> approvers = java.util.stream.IntStream.range(0, 11)
                .mapToObj(ignored -> UUID.randomUUID()).toList();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requestApproval(UUID.randomUUID(), UUID.randomUUID(),
                        new AwardApprovalRequest(UUID.randomUUID(), null, approvers)));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsEmptyApprovalRouteBeforeDatabaseAccess() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requestApproval(UUID.randomUUID(), UUID.randomUUID(),
                        new AwardApprovalRequest(UUID.randomUUID(), null, List.of())));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsUnsupportedDecisionBeforeDatabaseAccess() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.decide(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                        new ApprovalDecisionRequest("SKIP", "not a valid approval decision")));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void requiresCommentsWhenReturningForRevision() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.decide(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                        new ApprovalDecisionRequest("RETURN_FOR_REVISION", "  ")));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void requiresDecision() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.decide(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), null));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void requiresCommentsForApprovalDecision() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.decide(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                        new ApprovalDecisionRequest("APPROVE", "  ")));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }
}
