package com.srm.erp.supplier.service;

import com.srm.erp.supplier.api.SupplierRegistrationRequest;
import com.srm.erp.supplier.api.SupplierResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class SupplierService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public SupplierService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
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

    @Transactional
    public SupplierResponse registerSelf(SupplierRegistrationRequest request) {
        String password = request.password();
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Password must be at least 12 characters and no more than 72 UTF-8 bytes");
        }
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        Boolean emailExists = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM srm.app_user WHERE lower(email) = ?)
                """, Boolean.class, email);
        if (Boolean.TRUE.equals(emailExists)) {
            throw new ResponseStatusException(CONFLICT, "An account with this email already exists");
        }

        SupplierResponse supplier = register(request);
        UUID userId;
        try {
            userId = jdbc.queryForObject("""
                    INSERT INTO srm.app_user (email, password_hash, full_name, account_status)
                    VALUES (?, ?, ?, 'ACTIVE')
                    RETURNING user_id
                    """, UUID.class, email, passwordEncoder.encode(password), request.contactName().trim());
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(CONFLICT, "An account with this email already exists");
        }
        UUID roleId = jdbc.queryForObject(
                "SELECT role_id FROM srm.role WHERE role_code = 'SUPPLIER_USER'", UUID.class);
        jdbc.update("INSERT INTO srm.user_role (user_id, role_id, assigned_by) VALUES (?, ?, ?)",
                userId, roleId, userId);
        jdbc.update("""
                INSERT INTO srm.supplier_user_membership (supplier_id, user_id, membership_status)
                VALUES (?, ?, 'ACTIVE')
                """, supplier.supplierId(), userId);
        jdbc.update("""
                INSERT INTO srm.supplier_status_history
                    (supplier_id, old_status, new_status, changed_by, reason)
                VALUES (?, NULL, 'DRAFT', ?, 'Self-registration')
                """, supplier.supplierId(), userId);
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id,
                     outcome, changed_fields)
                VALUES (?, 'SUPPLIER_USER', 'SUPPLIER_SELF_REGISTERED', 'SUPPLIER', ?, ?, 'SUCCESS',
                        jsonb_build_object('lifecycle_status', 'DRAFT', 'account_status', 'ACTIVE'))
                """, userId, supplier.supplierId(), supplier.supplierId());
        return supplier;
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

    public List<SupplierResponse> findForUser(UUID userId) {
        return jdbc.query("""
                SELECT s.supplier_id, s.supplier_number, s.legal_name, s.trade_name,
                       s.gstin, s.pan, s.lifecycle_status, s.created_at,
                       c.full_name AS contact_name, c.email, c.phone
                FROM srm.supplier_user_membership m
                JOIN srm.supplier_organization s ON s.supplier_id = m.supplier_id
                LEFT JOIN srm.supplier_contact c
                  ON c.supplier_id = s.supplier_id AND c.contact_type = 'PRIMARY'
                WHERE m.user_id = ? AND m.membership_status = 'ACTIVE'
                ORDER BY s.legal_name
                """, supplierMapper, userId);
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
