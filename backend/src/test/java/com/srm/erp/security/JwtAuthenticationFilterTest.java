package com.srm.erp.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {
    private static final String TOKEN = "signed-token";
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000123");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesOnlyAnActiveAccount() throws Exception {
        JwtService jwt = mock(JwtService.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, jdbc);
        when(jwt.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<String>>any(), eq(USER_ID)))
                .thenReturn(List.of("PROCUREMENT_BUYER"));
        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilterInternal(request, response, chain);

        assertEquals(USER_ID.toString(), SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals("ROLE_PROCUREMENT_BUYER",
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority());
        verify(jdbc).query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<String>>any(), eq(USER_ID));
    }

    @Test
    void usesCurrentDatabaseRolesInsteadOfStaleTokenRoles() throws Exception {
        JwtService jwt = mock(JwtService.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, jdbc);
        when(jwt.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<String>>any(), eq(USER_ID)))
                .thenReturn(List.of("AUDITOR"));
        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("ROLE_AUDITOR",
                authentication.getAuthorities().iterator().next().getAuthority());
        verify(jwt, never()).extractRoles(TOKEN);
        verify(jdbc).query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<String>>any(), eq(USER_ID));
    }

    @Test
    void rejectsAlreadyIssuedTokenAfterAccountIsSuspended() throws Exception {
        JwtService jwt = mock(JwtService.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, jdbc);
        when(jwt.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<String>>any(), eq(USER_ID)))
                .thenReturn(List.of());
        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }
}
