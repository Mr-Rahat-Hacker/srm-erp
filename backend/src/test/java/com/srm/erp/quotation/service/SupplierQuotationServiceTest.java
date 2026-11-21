package com.srm.erp.quotation.service;

import com.srm.erp.quotation.api.QuotationLineRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SupplierQuotationServiceTest {
    @Test
    void calculatesLandedLineTotalFromUnitPriceQuantityAndCharges() {
        QuotationLineRequest line = new QuotationLineRequest(
                UUID.randomUUID(),
                new BigDecimal("100.0000"),
                new BigDecimal("5.00"),
                new BigDecimal("10.00"),
                new BigDecimal("3.00"),
                new BigDecimal("2.00"),
                null, null, null
        );

        assertEquals(new BigDecimal("252.50"),
                SupplierQuotationService.landedTotal(line, new BigDecimal("2.5000")));
    }

    @Test
    void supplierCannotReadRfqWithoutAnActiveInvitationMembership() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        doReturn(List.of()).when(jdbc).query(
                anyString(), any(RowMapper.class), any(UUID.class), any(UUID.class));

        SupplierQuotationService service = new SupplierQuotationService(jdbc);
        assertThrows(ResponseStatusException.class,
                () -> service.findRfq(UUID.randomUUID(), UUID.randomUUID()));
        verify(jdbc).query(anyString(), any(RowMapper.class), any(UUID.class), any(UUID.class));
    }
}
