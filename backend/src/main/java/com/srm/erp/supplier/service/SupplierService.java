package com.srm.erp.supplier.service;

import com.srm.erp.supplier.api.SupplierRegistrationRequest;
import com.srm.erp.supplier.api.SupplierResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class SupplierService {
    private final JdbcTemplate jdbc;

    public SupplierService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<SupplierResponse> supplierMapper = this::mapSupplier;

    @Transactional
    public SupplierResponse register(SupplierRegistrationRequest request) {
        boolean hasAddress = request.addressLine1() != null && !request.addressLine1().isBlank();
        if (hasAddress && (request.city() == null || request.city().isBlank()
                || request.countryCode() == null || request.countryCode().isBlank())) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "City and country code are required when an address is provided");
        }

        Long sequence = jdbc.queryForObject("SELECT nextval('srm.supplier_number_seq')", Long.class);
        String supplierNumber = "SUP-" + sequence;
        UUID supplierId = jdbc.queryForObject("""
                INSERT INTO srm.supplier_organization
                    (supplier_number, legal_name, trade_name, gstin, pan, lifecycle_status)
                VALUES (?, ?, ?, ?, ?, 'DRAFT')
                RETURNING supplier_id
                """, UUID.class, supplierNumber, request.legalName().trim(),
                blankToNull(request.tradeName()), blankToNull(request.gstin()),
                blankToNull(request.pan()));

        jdbc.update("""
                INSERT INTO srm.supplier_contact
                    (supplier_id, full_name, email, phone, contact_type)
                VALUES (?, ?, ?, ?, 'PRIMARY')
                """, supplierId, request.contactName().trim(), request.email().trim(),
                blankToNull(request.phone()));

        if (hasAddress) {
            jdbc.update("""
                    INSERT INTO srm.supplier_address
                        (supplier_id, address_type, address_line_1, city, state_region, postal_code, country_code, is_primary)
                    VALUES (?, 'REGISTERED', ?, ?, ?, ?, ?, true)
                    """, supplierId, request.addressLine1().trim(), request.city().trim(),
                    blankToNull(request.stateRegion()), blankToNull(request.postalCode()),
                    request.countryCode().trim().toUpperCase());
        }

        return findByIdPublic(supplierId);
    }

    public List<SupplierResponse> findAll() {
        return jdbc.query("""
                SELECT s.supplier_id, s.supplier_number, s.legal_name, s.trade_name,
                       s.gstin, s.pan, s.lifecycle_status, s.created_at,
                       c.full_name AS contact_name, c.email, c.phone
                FROM srm.supplier_organization s
                LEFT JOIN srm.supplier_contact c
                  ON c.supplier_id = s.supplier_id AND c.contact_type = 'PRIMARY'
                ORDER BY s.created_at DESC
                """, supplierMapper);
    }

    public SupplierResponse findByIdPublic(UUID supplierId) {
        return jdbc.queryForObject("""
                SELECT s.supplier_id, s.supplier_number, s.legal_name, s.trade_name,
                       s.gstin, s.pan, s.lifecycle_status, s.created_at,
                       c.full_name AS contact_name, c.email, c.phone
                FROM srm.supplier_organization s
                LEFT JOIN srm.supplier_contact c
                  ON c.supplier_id = s.supplier_id AND c.contact_type = 'PRIMARY'
                WHERE s.supplier_id = ?
                """, supplierMapper, supplierId);
    }

    private SupplierResponse mapSupplier(ResultSet rs, int rowNum) throws SQLException {
        return new SupplierResponse(
                rs.getObject("supplier_id", UUID.class),
                rs.getString("supplier_number"),
                rs.getString("legal_name"),
                rs.getString("trade_name"),
                rs.getString("gstin"),
                rs.getString("pan"),
                rs.getString("lifecycle_status"),
                rs.getString("contact_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
