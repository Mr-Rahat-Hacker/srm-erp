package com.srm.erp.quotation.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class QuotationComparisonServiceTest {
    @Test
    void buyerCannotCompareAnotherBuyersRfq() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        doReturn(List.of()).when(jdbc).query(
                anyString(), any(RowMapper.class), any(UUID.class), any(UUID.class));

        QuotationComparisonService service = new QuotationComparisonService(jdbc);
        assertThrows(ResponseStatusException.class,
                () -> service.compare(UUID.randomUUID(), UUID.randomUUID()));
        verify(jdbc).query(anyString(), any(RowMapper.class), any(UUID.class), any(UUID.class));
    }
}
