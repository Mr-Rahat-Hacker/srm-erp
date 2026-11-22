package com.srm.erp.platform.response;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PagedListControllerTest {
    @Test
    void rejectsPageSizesOutsideTheSupportedRangeBeforeDatabaseAccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PagedListController controller = new PagedListController(jdbc);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.suppliers(0, 101, "", ""));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(jdbc);
    }

    @Test
    void appliesBuyerScopeAndFiltersToBothRfqQueries() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        doReturn(1L).when(jdbc).queryForObject(anyString(), eq(Long.class), any(Object[].class));
        doReturn(List.of()).when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
        PagedListController controller = new PagedListController(jdbc);
        UUID buyerId = UUID.randomUUID();
        var authentication = new UsernamePasswordAuthenticationToken(
                buyerId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_PROCUREMENT_BUYER")));

        ApiResponse<PageResponse<com.srm.erp.rfq.api.RfqSummaryResponse>> response =
                controller.rfqs(authentication, 1, 10, "pump", "OPEN");

        assertEquals(1, response.data().totalElements());
        assertEquals(1, response.data().page());
        verify(jdbc).queryForObject(
                contains("WHERE (? = false OR r.buyer_user_id = ?)"),
                eq(Long.class), eq(true), eq(buyerId), eq("pump"), eq("%pump%"),
                eq("%pump%"), eq("OPEN"), eq("OPEN"));
        verify(jdbc).query(
                contains("WHERE (? = false OR r.buyer_user_id = ?)"),
                any(RowMapper.class), eq(true), eq(buyerId), eq("pump"), eq("%pump%"),
                eq("%pump%"), eq("OPEN"), eq("OPEN"), eq(10), eq(10L));
    }
}
