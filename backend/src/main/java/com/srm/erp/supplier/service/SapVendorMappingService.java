package com.srm.erp.supplier.service;

import com.srm.erp.supplier.api.SapVendorMappingRequest;
import com.srm.erp.supplier.api.SapVendorMappingResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SapVendorMappingService {
    private final JdbcTemplate jdbc;

    public SapVendorMappingService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SapVendorMappingResponse> findForSupplier(UUID supplierId) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM srm.supplier_organization WHERE supplier_id = ?)
                """, Boolean.class, supplierId);
        if (!Boolean.TRUE.equals(exists)) throw new ResponseStatusException(NOT_FOUND, "Supplier not found");
        return jdbc.query("""
                SELECT supplier_id, sap_system_code, sap_vendor_code, is_active
                FROM srm.sap_vendor_mapping
                WHERE supplier_id = ?
                ORDER BY sap_system_code
                """, (rs, row) -> new SapVendorMappingResponse(
                rs.getObject("supplier_id", UUID.class), rs.getString("sap_system_code"),
                rs.getString("sap_vendor_code"), rs.getBoolean("is_active")), supplierId);
    }

    @Transactional
    public SapVendorMappingResponse save(UUID supplierId, UUID actorId, SapVendorMappingRequest request) {
        String systemCode = request.sapSystemCode().trim().toUpperCase(Locale.ROOT);
        String vendorCode = request.sapVendorCode().trim();
        String reason = request.reason().trim();
        if (systemCode.isBlank() || vendorCode.isBlank() || reason.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "System code, vendor code and reason are required");
        }

        List<String> statuses = jdbc.query("""
                SELECT lifecycle_status FROM srm.supplier_organization WHERE supplier_id = ?
                """, (rs, row) -> rs.getString(1), supplierId);
        if (statuses.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Supplier not found");
        if (!"APPROVED".equals(statuses.getFirst())) {
            throw new ResponseStatusException(CONFLICT,
                    "SAP Vendor Code can only be mapped to an approved supplier");
        }

        Boolean assigned = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM srm.sap_vendor_mapping
                    WHERE sap_system_code = ? AND sap_vendor_code = ? AND is_active = true
                      AND supplier_id <> ?)
                """, Boolean.class, systemCode, vendorCode, supplierId);
        if (Boolean.TRUE.equals(assigned)) {
            throw new ResponseStatusException(CONFLICT,
                    "This SAP Vendor Code is already mapped to another supplier in the selected system");
        }

        List<String> existingCodes = jdbc.query("""
                SELECT sap_vendor_code FROM srm.sap_vendor_mapping
                WHERE supplier_id = ? AND sap_system_code = ?
                """, (rs, row) -> rs.getString(1), supplierId, systemCode);
        String oldCode = existingCodes.isEmpty() ? null : existingCodes.getFirst();
        try {
            if (oldCode == null) {
                jdbc.update("""
                        INSERT INTO srm.sap_vendor_mapping
                            (supplier_id, sap_system_code, sap_vendor_code, is_active)
                        VALUES (?, ?, ?, true)
                        """, supplierId, systemCode, vendorCode);
            } else {
                jdbc.update("""
                        UPDATE srm.sap_vendor_mapping
                        SET sap_vendor_code = ?, is_active = true
                        WHERE supplier_id = ? AND sap_system_code = ?
                        """, vendorCode, supplierId, systemCode);
            }
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(CONFLICT,
                    "This SAP Vendor Code is already mapped in the selected SAP system");
        }

        String action = oldCode == null ? "SAP_VENDOR_MAPPING_CREATED" : "SAP_VENDOR_MAPPING_UPDATED";
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id,
                     outcome, reason, changed_fields)
                VALUES (?, 'USER', ?, 'SUPPLIER', ?, ?, 'SUCCESS', ?,
                        jsonb_build_object('sap_system_code', ?, 'sap_vendor_code',
                                           jsonb_build_object('old', ?, 'new', ?)))
                """, actorId, action, supplierId, supplierId, reason, systemCode, oldCode, vendorCode);
        return new SapVendorMappingResponse(supplierId, systemCode, vendorCode, true);
    }

    @Transactional
    public void deactivate(UUID supplierId, String rawSystemCode, UUID actorId, String reason) {
        String systemCode = rawSystemCode == null ? "" : rawSystemCode.trim().toUpperCase(Locale.ROOT);
        if (systemCode.isBlank() || reason == null || reason.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "SAP system code and deactivation reason are required");
        }
        List<String> vendorCodes = jdbc.query("""
                SELECT sap_vendor_code FROM srm.sap_vendor_mapping
                WHERE supplier_id = ? AND sap_system_code = ? AND is_active = true
                """, (rs, row) -> rs.getString(1), supplierId, systemCode);
        if (vendorCodes.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Active SAP mapping not found");

        jdbc.update("""
                UPDATE srm.sap_vendor_mapping SET is_active = false
                WHERE supplier_id = ? AND sap_system_code = ? AND is_active = true
                """, supplierId, systemCode);
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id,
                     outcome, reason, changed_fields)
                VALUES (?, 'USER', 'SAP_VENDOR_MAPPING_DEACTIVATED', 'SUPPLIER', ?, ?, 'SUCCESS', ?,
                        jsonb_build_object('sap_system_code', ?, 'sap_vendor_code', ?))
                """, actorId, supplierId, supplierId, reason.trim(), systemCode, vendorCodes.getFirst());
    }
}
