package com.srm.erp.supplier.service;

import com.srm.erp.supplier.api.SupplierProfileUpdateRequest;
import com.srm.erp.supplier.api.SupplierResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SupplierProfileService {
    private final JdbcTemplate jdbc;
    private final SupplierService suppliers;

    public SupplierProfileService(JdbcTemplate jdbc, SupplierService suppliers) {
        this.jdbc = jdbc;
        this.suppliers = suppliers;
    }

    @Transactional
    public SupplierResponse updateForUser(UUID supplierId, UUID userId, SupplierProfileUpdateRequest request) {
        List<String> statuses = jdbc.query("""
                SELECT s.lifecycle_status
                FROM srm.supplier_organization s
                JOIN srm.supplier_user_membership m ON m.supplier_id = s.supplier_id
                WHERE s.supplier_id = ? AND m.user_id = ? AND m.membership_status = 'ACTIVE'
                """, (rs, row) -> rs.getString(1), supplierId, userId);
        if (statuses.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Supplier not found");

        String status = statuses.getFirst();
        if (!"DRAFT".equals(status) && !"CORRECTION_REQUIRED".equals(status)) {
            throw new ResponseStatusException(CONFLICT,
                    "Supplier profile can only be changed while draft or correction-required");
        }
        validate(request);
        List<String> fields = suppliedFields(request);
        if (fields.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "Provide at least one profile field");

        boolean hasAddressInput = hasText(request.addressLine1()) || hasText(request.city())
                || hasText(request.stateRegion()) || hasText(request.postalCode()) || hasText(request.countryCode());
        Boolean addressExists = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM srm.supplier_address
                    WHERE supplier_id = ? AND address_type = 'REGISTERED' AND is_primary = true)
                """, Boolean.class, supplierId);
        if (hasAddressInput && !Boolean.TRUE.equals(addressExists)
                && (!hasText(request.addressLine1()) || !hasText(request.city()) || !hasText(request.countryCode()))) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Address line, city and country code are required when adding a registered address");
        }

        jdbc.update("""
                UPDATE srm.supplier_organization
                SET legal_name = COALESCE(?, legal_name),
                    trade_name = COALESCE(?, trade_name),
                    gstin = COALESCE(?, gstin),
                    pan = COALESCE(?, pan),
                    updated_at = now()
                WHERE supplier_id = ?
                """, trimOrNull(request.legalName()), trimOrNull(request.tradeName()),
                trimOrNull(request.gstin()), trimOrNull(request.pan()), supplierId);
        jdbc.update("""
                UPDATE srm.supplier_contact
                SET full_name = COALESCE(?, full_name),
                    email = COALESCE(?, email),
                    phone = COALESCE(?, phone)
                WHERE supplier_id = ? AND contact_type = 'PRIMARY'
                """, trimOrNull(request.contactName()), trimOrNull(request.email()),
                trimOrNull(request.phone()), supplierId);

        if (Boolean.TRUE.equals(addressExists) && hasAddressInput) {
            jdbc.update("""
                    UPDATE srm.supplier_address
                    SET address_line_1 = COALESCE(?, address_line_1),
                        city = COALESCE(?, city),
                        state_region = COALESCE(?, state_region),
                        postal_code = COALESCE(?, postal_code),
                        country_code = COALESCE(?, country_code)
                    WHERE supplier_id = ? AND address_type = 'REGISTERED' AND is_primary = true
                    """, trimOrNull(request.addressLine1()), trimOrNull(request.city()),
                    trimOrNull(request.stateRegion()), trimOrNull(request.postalCode()),
                    upperOrNull(request.countryCode()), supplierId);
        } else if (hasAddressInput) {
            jdbc.update("""
                    INSERT INTO srm.supplier_address
                        (supplier_id, address_type, address_line_1, city, state_region, postal_code,
                         country_code, is_primary)
                    VALUES (?, 'REGISTERED', ?, ?, ?, ?, ?, true)
                    """, supplierId, trimOrNull(request.addressLine1()), trimOrNull(request.city()),
                    trimOrNull(request.stateRegion()), trimOrNull(request.postalCode()),
                    upperOrNull(request.countryCode()));
        }

        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id,
                     outcome, changed_fields)
                VALUES (?, 'SUPPLIER_USER', 'SUPPLIER_PROFILE_UPDATED', 'SUPPLIER', ?, ?, 'SUCCESS',
                        jsonb_build_object('updated_fields', ?))
                """, userId, supplierId, supplierId, String.join(",", fields));
        return suppliers.findByIdPublic(supplierId);
    }

    private void validate(SupplierProfileUpdateRequest request) {
        if (request.legalName() != null && request.legalName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Legal name cannot be blank");
        }
        if (request.contactName() != null && request.contactName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Contact name cannot be blank");
        }
        if (request.email() != null && request.email().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Contact email cannot be blank");
        }
    }

    private List<String> suppliedFields(SupplierProfileUpdateRequest request) {
        List<String> fields = new ArrayList<>();
        if (request.legalName() != null) fields.add("legal_name");
        if (request.tradeName() != null) fields.add("trade_name");
        if (request.gstin() != null) fields.add("gstin");
        if (request.pan() != null) fields.add("pan");
        if (request.contactName() != null) fields.add("contact_name");
        if (request.email() != null) fields.add("contact_email");
        if (request.phone() != null) fields.add("phone");
        if (request.addressLine1() != null) fields.add("address_line_1");
        if (request.city() != null) fields.add("city");
        if (request.stateRegion() != null) fields.add("state_region");
        if (request.postalCode() != null) fields.add("postal_code");
        if (request.countryCode() != null) fields.add("country_code");
        return fields;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String upperOrNull(String value) {
        String cleaned = trimOrNull(value);
        return cleaned == null ? null : cleaned.toUpperCase(java.util.Locale.ROOT);
    }
}
