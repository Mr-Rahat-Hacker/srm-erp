package com.srm.erp.supplier.service;

import com.srm.erp.supplier.api.SupplierDocumentResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SupplierDocumentService {
    private final JdbcTemplate jdbc;
    private final SupplierDocumentStorageService storage;

    public SupplierDocumentService(JdbcTemplate jdbc, SupplierDocumentStorageService storage) {
        this.jdbc = jdbc;
        this.storage = storage;
    }

    @Transactional
    public SupplierDocumentResponse upload(UUID supplierId, UUID actorId, String documentType,
                                           String filename, String contentType, String storageKey,
                                           LocalDate expiryDate) {
        if (jdbc.queryForObject("SELECT count(*) FROM srm.supplier_organization WHERE supplier_id = ?",
                Integer.class, supplierId) == 0) {
            storage.deleteQuietly(storageKey);
            throw new ResponseStatusException(NOT_FOUND, "Supplier not found");
        }
        String type = documentType == null ? "" : documentType.trim().toUpperCase();
        if (!List.of("GST_CERTIFICATE", "PAN_CARD", "ISO_CERTIFICATE", "BANK_PROOF",
                "MSME_CERTIFICATE", "OTHER").contains(type)) {
            storage.deleteQuietly(storageKey);
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported document type");
        }
        Integer version = jdbc.queryForObject("""
                SELECT COALESCE(MAX(version_number), 0) + 1
                FROM srm.supplier_document
                WHERE supplier_id = ? AND document_type = ?
                """, Integer.class, supplierId, type);
        UUID documentId = jdbc.queryForObject("""
                INSERT INTO srm.supplier_document
                    (supplier_id, document_type, storage_key, original_filename, content_type,
                     version_number, review_status, expiry_date, uploaded_by)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
                RETURNING document_id
                """, UUID.class, supplierId, type, storageKey, safeFilename(filename),
                contentType, version, expiryDate, actorId);

        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id,
                     outcome, changed_fields)
                VALUES (?, 'SRM_ADMIN', 'SUPPLIER_DOCUMENT_UPLOADED', 'SUPPLIER_DOCUMENT',
                        ?, ?, 'SUCCESS', jsonb_build_object('document_type', ?, 'version_number', ?))
                """, actorId, documentId, supplierId, type, version);
        return findOne(supplierId, documentId);
    }

    public List<SupplierDocumentResponse> findAll(UUID supplierId) {
        ensureSupplierExists(supplierId);
        return jdbc.query("""
                SELECT document_id, supplier_id, document_type, original_filename, content_type,
                       version_number, review_status, expiry_date, review_remarks, uploaded_at, reviewed_at
                FROM srm.supplier_document
                WHERE supplier_id = ?
                ORDER BY uploaded_at DESC
                """, this::map, supplierId);
    }

    public DocumentFile findFile(UUID supplierId, UUID documentId) {
        return jdbc.query("""
                SELECT storage_key, original_filename, content_type
                FROM srm.supplier_document
                WHERE supplier_id = ? AND document_id = ?
                """, rs -> {
            if (!rs.next()) throw new ResponseStatusException(NOT_FOUND, "Supplier document not found");
            return new DocumentFile(rs.getString("storage_key"), rs.getString("original_filename"),
                    rs.getString("content_type"));
        }, supplierId, documentId);
    }

    @Transactional
    public SupplierDocumentResponse review(UUID supplierId, UUID documentId, UUID actorId,
                                           String decision, String remarks) {
        String status = switch (decision == null ? "" : decision.trim().toUpperCase()) {
            case "VERIFY" -> "VERIFIED";
            case "REJECT" -> "REJECTED";
            default -> throw new ResponseStatusException(BAD_REQUEST, "Decision must be VERIFY or REJECT");
        };
        if ("REJECTED".equals(status) && (remarks == null || remarks.isBlank())) {
            throw new ResponseStatusException(BAD_REQUEST, "Remarks are required when rejecting a document");
        }
        int updated = jdbc.update("""
                UPDATE srm.supplier_document
                SET review_status = ?, reviewed_by = ?, reviewed_at = now(), review_remarks = ?
                WHERE supplier_id = ? AND document_id = ? AND review_status = 'PENDING'
                """, status, actorId, remarks, supplierId, documentId);
        if (updated != 1) {
            throw new ResponseStatusException(NOT_FOUND, "Pending supplier document not found");
        }
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id,
                     outcome, reason, changed_fields)
                VALUES (?, 'SUPPLIER_VERIFIER', ?, 'SUPPLIER_DOCUMENT', ?, ?, 'SUCCESS', ?,
                        jsonb_build_object('review_status', ?))
                """, actorId, "SUPPLIER_DOCUMENT_" + status, documentId, supplierId, remarks, status);
        return findOne(supplierId, documentId);
    }

    private SupplierDocumentResponse findOne(UUID supplierId, UUID documentId) {
        return jdbc.queryForObject("""
                SELECT document_id, supplier_id, document_type, original_filename, content_type,
                       version_number, review_status, expiry_date, review_remarks, uploaded_at, reviewed_at
                FROM srm.supplier_document WHERE supplier_id = ? AND document_id = ?
                """, this::map, supplierId, documentId);
    }

    private void ensureSupplierExists(UUID supplierId) {
        if (jdbc.queryForObject("SELECT count(*) FROM srm.supplier_organization WHERE supplier_id = ?",
                Integer.class, supplierId) == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Supplier not found");
        }
    }

    private SupplierDocumentResponse map(ResultSet rs, int rowNum) throws SQLException {
        return new SupplierDocumentResponse(rs.getObject("document_id", UUID.class),
                rs.getObject("supplier_id", UUID.class), rs.getString("document_type"),
                rs.getString("original_filename"), rs.getString("content_type"),
                rs.getInt("version_number"), rs.getString("review_status"),
                rs.getObject("expiry_date", LocalDate.class), rs.getString("review_remarks"),
                rs.getTimestamp("uploaded_at").toInstant(),
                rs.getTimestamp("reviewed_at") == null ? null : rs.getTimestamp("reviewed_at").toInstant());
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "supplier-document";
        String clean = filename.replaceAll("[\\\\/\\r\\n\\\\p{Cntrl}]", "_");
        return clean.length() > 255 ? clean.substring(clean.length() - 255) : clean;
    }

    public record DocumentFile(String storageKey, String filename, String contentType) {}
}
