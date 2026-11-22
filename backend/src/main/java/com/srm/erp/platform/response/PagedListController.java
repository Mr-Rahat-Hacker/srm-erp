package com.srm.erp.platform.response;

import com.srm.erp.rfq.api.RfqSummaryResponse;
import com.srm.erp.supplier.api.SupplierResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
public class PagedListController {
    private final JdbcTemplate jdbc;

    public PagedListController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/v1/suppliers/page")
    @PreAuthorize("hasAnyRole('PROCUREMENT_BUYER','SUPPLIER_VERIFIER','SRM_ADMIN','SUPER_ADMIN','AUDITOR')")
    public ApiResponse<PageResponse<SupplierResponse>> suppliers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String status) {
        validate(page, size);
        String term = contains(q);
        String lifecycle = status == null ? "" : status.trim();
        String where = """
                WHERE (? = '' OR s.legal_name ILIKE ? OR COALESCE(s.trade_name, '') ILIKE ?
                    OR s.supplier_number ILIKE ? OR COALESCE(c.email, '') ILIKE ?)
                  AND (? = '' OR s.lifecycle_status = ?)
                """;
        Long total = jdbc.queryForObject("""
                SELECT count(*) FROM srm.supplier_organization s
                LEFT JOIN srm.supplier_contact c
                  ON c.supplier_id = s.supplier_id AND c.contact_type = 'PRIMARY'
                """ + where, Long.class, q.trim(), term, term, term, term, lifecycle, lifecycle);
        List<SupplierResponse> items = jdbc.query("""
                SELECT s.supplier_id, s.supplier_number, s.legal_name, s.trade_name,
                       s.gstin, s.pan, s.lifecycle_status, s.created_at,
                       c.full_name AS contact_name, c.email, c.phone
                FROM srm.supplier_organization s
                LEFT JOIN srm.supplier_contact c
                  ON c.supplier_id = s.supplier_id AND c.contact_type = 'PRIMARY'
                """ + where + """
                ORDER BY s.created_at DESC, s.supplier_id
                LIMIT ? OFFSET ?
                """, (rs, row) -> new SupplierResponse(
                rs.getObject("supplier_id", UUID.class), rs.getString("supplier_number"),
                rs.getString("legal_name"), rs.getString("trade_name"), rs.getString("gstin"),
                rs.getString("pan"), rs.getString("lifecycle_status"), rs.getString("contact_name"),
                rs.getString("email"), rs.getString("phone"), rs.getTimestamp("created_at").toInstant()
        ), q.trim(), term, term, term, term, lifecycle, lifecycle, size, (long) page * size);
        return ApiResponse.success("Suppliers retrieved successfully",
                PageResponse.of(items, page, size, total == null ? 0 : total));
    }

    @GetMapping("/api/v1/rfqs/page")
    @PreAuthorize("hasAnyRole('PROCUREMENT_BUYER','SUPPLIER_VERIFIER','SRM_ADMIN','SUPER_ADMIN','AUDITOR')")
    public ApiResponse<PageResponse<RfqSummaryResponse>> rfqs(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String status) {
        validate(page, size);
        UUID actorId = UUID.fromString(authentication.getName());
        boolean buyerOnly = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_PROCUREMENT_BUYER".equals(a.getAuthority()))
                && authentication.getAuthorities().stream().noneMatch(a -> List.of(
                "ROLE_SUPPLIER_VERIFIER", "ROLE_SRM_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_AUDITOR")
                .contains(a.getAuthority()));
        String term = contains(q);
        String lifecycle = status == null ? "" : status.trim();
        String where = """
                WHERE (? = false OR r.buyer_user_id = ?)
                  AND (? = '' OR r.rfq_number ILIKE ? OR r.title ILIKE ?)
                  AND (? = '' OR r.status = ?)
                """;
        Object[] filterArgs = {buyerOnly, actorId, q.trim(), term, term, lifecycle, lifecycle};
        Long total = jdbc.queryForObject("""
                SELECT count(*) FROM srm.rfq r
                """ + where, Long.class, filterArgs);
        List<RfqSummaryResponse> items = jdbc.query("""
                SELECT r.rfq_id, r.rfq_number, r.title, r.status, r.currency_code,
                       r.submission_deadline, r.created_at,
                       (SELECT count(*) FROM srm.rfq_line l WHERE l.rfq_id = r.rfq_id) AS line_count,
                       (SELECT count(*) FROM srm.rfq_invitation i WHERE i.rfq_id = r.rfq_id) AS invitation_count
                FROM srm.rfq r
                """ + where + """
                ORDER BY r.created_at DESC, r.rfq_id
                LIMIT ? OFFSET ?
                """, (rs, row) -> new RfqSummaryResponse(
                rs.getObject("rfq_id", UUID.class), rs.getString("rfq_number"), rs.getString("title"),
                rs.getString("status"), rs.getString("currency_code"),
                rs.getTimestamp("submission_deadline").toInstant(), rs.getInt("line_count"),
                rs.getInt("invitation_count"), rs.getTimestamp("created_at").toInstant()
        ), buyerOnly, actorId, q.trim(), term, term, lifecycle, lifecycle, size, (long) page * size);
        return ApiResponse.success("RFQs retrieved successfully",
                PageResponse.of(items, page, size, total == null ? 0 : total));
    }

    private void validate(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        }
    }

    private String contains(String value) {
        return "%" + value.trim().replace("%", "\\%").replace("_", "\\_") + "%";
    }

}
