package com.srm.erp.platform.response;

import com.srm.erp.rfq.api.RfqSummaryResponse;
import com.srm.erp.supplier.api.SupplierResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.task.scheduling.enabled=false"
})
@Transactional
class PagedListPostgresIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PagedListController lists;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supplierPageRunsAgainstPostgresWithStatusAndTextFilters() {
        authenticate(UUID.randomUUID(), "SUPPLIER_VERIFIER");
        UUID supplierId = UUID.randomUUID();
        String number = "SUP-" + supplierId.toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO srm.supplier_organization
                    (supplier_id, supplier_number, legal_name, lifecycle_status)
                VALUES (?, ?, ?, 'APPROVED')
                """, supplierId, number, "Acme Paged Components");

        ApiResponse<PageResponse<SupplierResponse>> response =
                lists.suppliers(0, 10, "Acme Paged", "APPROVED");

        assertEquals(1, response.data().totalElements());
        assertEquals(supplierId, response.data().items().getFirst().supplierId());
        assertEquals(1, response.data().totalPages());
    }

    @Test
    void buyerRfqPageRunsAgainstPostgresAndEnforcesBuyerScope() {
        UUID buyerId = UUID.randomUUID();
        authenticate(buyerId, "PROCUREMENT_BUYER");
        UUID rfqId = UUID.randomUUID();
        String suffix = buyerId.toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO srm.app_user (user_id, email, password_hash, full_name, account_status)
                VALUES (?, ?, ?, ?, 'ACTIVE')
                """, buyerId, "buyer-" + suffix + "@example.test", "test-password-hash",
                "Pagination Buyer");
        jdbc.update("""
                INSERT INTO srm.rfq
                    (rfq_id, rfq_number, title, buyer_user_id, currency_code,
                     submission_deadline, status)
                VALUES (?, ?, ?, ?, 'INR', ?, 'DRAFT')
                """, rfqId, "RFQ-" + suffix, "Pagination Test RFQ", buyerId,
                Timestamp.from(Instant.now().plusSeconds(3600)));

        var buyer = new UsernamePasswordAuthenticationToken(
                buyerId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROCUREMENT_BUYER")));
        ApiResponse<PageResponse<RfqSummaryResponse>> response =
                lists.rfqs(buyer, 0, 10, "Pagination Test", "DRAFT");
        var otherBuyer = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROCUREMENT_BUYER")));
        ApiResponse<PageResponse<RfqSummaryResponse>> hidden =
                lists.rfqs(otherBuyer, 0, 10, "", "");

        assertEquals(1, response.data().totalElements());
        assertEquals(rfqId, response.data().items().getFirst().rfqId());
        assertEquals(0, hidden.data().totalElements());
        assertEquals(0, hidden.data().items().size());
    }

    private void authenticate(UUID userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
