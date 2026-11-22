package com.srm.erp.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final JdbcTemplate jdbc;

    public JwtAuthenticationFilter(JwtService jwtService, JdbcTemplate jdbc) {
        this.jwtService = jwtService;
        this.jdbc = jdbc;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                var userId = jwtService.extractUserId(token);
                List<String> currentRoles = jdbc.query("""
                        SELECT r.role_code
                        FROM srm.app_user u
                        JOIN srm.user_role ur ON ur.user_id = u.user_id
                        JOIN srm.role r ON r.role_id = ur.role_id
                        WHERE u.user_id = ? AND u.account_status = 'ACTIVE'
                        ORDER BY r.role_code
                        """, (rs, row) -> rs.getString("role_code"), userId);
                if (currentRoles.isEmpty()) {
                    SecurityContextHolder.clearContext();
                } else {
                    List<SimpleGrantedAuthority> authorities = currentRoles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userId.toString(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
