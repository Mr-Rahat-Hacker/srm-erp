package com.srm.erp.negotiation.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NegotiationServiceTest {
    @Test
    void buyerCannotStartNegotiationForAnotherBuyersRfq() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(Integer.class),
                org.mockito.ArgumentMatchers.any(UUID.class), org.mockito.ArgumentMatchers.any(UUID.class)))
                .thenReturn(0);

        NegotiationService service = new NegotiationService(jdbc);
        assertThrows(ResponseStatusException.class,
                () -> service.listForBuyer(UUID.randomUUID(), UUID.randomUUID()));
    }
}
