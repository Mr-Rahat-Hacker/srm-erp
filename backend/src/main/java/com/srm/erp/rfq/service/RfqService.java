package com.srm.erp.rfq.service;

import com.srm.erp.rfq.api.RfqCreateRequest;
import com.srm.erp.rfq.api.RfqInvitationResponse;
import com.srm.erp.rfq.api.RfqLineRequest;
import com.srm.erp.rfq.api.RfqLineResponse;
import com.srm.erp.rfq.api.RfqResponse;
import com.srm.erp.rfq.api.RfqSummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RfqService {
    private final JdbcTemplate jdbc;

    public RfqService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public RfqResponse create(UUID buyerId, RfqCreateRequest request) {
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "An RFQ must contain at least one line");
        }
        if (request.submissionDeadline() == null || !request.submissionDeadline().isAfter(Instant.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "Submission deadline must be in the future");
        }

        Long sequence = jdbc.queryForObject("SELECT nextval('srm.rfq_number_seq')", Long.class);
        String rfqNumber = "RFQ-" + String.format("%09d", sequence);
        UUID rfqId = jdbc.queryForObject("""
                INSERT INTO srm.rfq
                    (rfq_number, title, description, buyer_user_id, project_code, department,
                     plant_code, purchasing_group, currency_code, submission_deadline,
                     required_delivery_date, payment_terms, incoterms, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                RETURNING rfq_id
                """, UUID.class, rfqNumber, request.title().trim(), blankToNull(request.description()),
                buyerId, blankToNull(request.projectCode()), blankToNull(request.department()),
                blankToNull(request.plantCode()), blankToNull(request.purchasingGroup()),
                request.currencyCode(), request.submissionDeadline(), request.requiredDeliveryDate(),
                blankToNull(request.paymentTerms()), blankToNull(request.incoterms()));

        int lineNumber = 1;
        for (RfqLineRequest line : request.lines()) {
            insertLine(rfqId, lineNumber++, line);
        }
        writeAudit(buyerId, "RFQ_CREATED", rfqId, null, rfqNumber, "DRAFT");
        return findOne(rfqId, buyerId, true);
    }

    public List<RfqSummaryResponse> findAll(UUID actorId, boolean restrictToBuyer) {
        return jdbc.query("""
                SELECT r.rfq_id, r.rfq_number, r.title, r.status, r.currency_code,
                       r.submission_deadline, r.created_at,
                       (SELECT count(*) FROM srm.rfq_line l WHERE l.rfq_id = r.rfq_id) AS line_count,
                       (SELECT count(*) FROM srm.rfq_invitation i WHERE i.rfq_id = r.rfq_id) AS invitation_count
                FROM srm.rfq r
                WHERE (? = false OR r.buyer_user_id = ?)
                ORDER BY r.created_at DESC
                """, (rs, row) -> new RfqSummaryResponse(
                rs.getObject("rfq_id", UUID.class), rs.getString("rfq_number"), rs.getString("title"),
                rs.getString("status"), rs.getString("currency_code"), rs.getTimestamp("submission_deadline").toInstant(),
                rs.getInt("line_count"), rs.getInt("invitation_count"), rs.getTimestamp("created_at").toInstant()
        ), restrictToBuyer, actorId);
    }

    public RfqResponse findOne(UUID rfqId, UUID actorId, boolean restrictToBuyer) {
        RfqHeader header = findHeader(rfqId, actorId, restrictToBuyer);
        List<RfqLineResponse> lines = jdbc.query("""
                SELECT rfq_line_id, line_number, sap_material_code, description, drawing_revision,
                       quantity, uom_code, plant_code, technical_specifications
                FROM srm.rfq_line WHERE rfq_id = ? ORDER BY line_number
                """, this::mapLine, rfqId);
        List<RfqInvitationResponse> invitations = jdbc.query("""
                SELECT i.supplier_id, s.supplier_number, s.legal_name, i.invitation_status, i.invited_at
                FROM srm.rfq_invitation i
                JOIN srm.supplier_organization s ON s.supplier_id = i.supplier_id
                WHERE i.rfq_id = ? ORDER BY i.invited_at, s.legal_name
                """, this::mapInvitation, rfqId);
        return new RfqResponse(header.rfqId(), header.rfqNumber(), header.title(), header.description(),
                header.projectCode(), header.department(), header.plantCode(), header.purchasingGroup(),
                header.currencyCode(), header.submissionDeadline(), header.requiredDeliveryDate(),
                header.paymentTerms(), header.incoterms(), header.status(), header.createdAt(), lines, invitations);
    }

    @Transactional
    public List<RfqInvitationResponse> invite(UUID rfqId, UUID buyerId, List<UUID> supplierIds) {
        RfqHeader header = findHeader(rfqId, buyerId, true);
        if (!"DRAFT".equals(header.status())) {
            throw new ResponseStatusException(CONFLICT, "Suppliers can only be invited while an RFQ is a draft");
        }
        if (supplierIds == null || supplierIds.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Select at least one supplier to invite");
        }
        if (new HashSet<>(supplierIds).size() != supplierIds.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "A supplier can only be selected once");
        }
        for (UUID supplierId : supplierIds) {
            List<String> statuses = jdbc.query("""
                    SELECT lifecycle_status FROM srm.supplier_organization WHERE supplier_id = ?
                    """, (rs, row) -> rs.getString(1), supplierId);
            if (statuses.isEmpty()) {
                throw new ResponseStatusException(NOT_FOUND, "Supplier not found: " + supplierId);
            }
            if (!"APPROVED".equals(statuses.getFirst()) && !"ACTIVE".equals(statuses.getFirst())) {
                throw new ResponseStatusException(BAD_REQUEST, "Only approved suppliers can be invited");
            }
            Integer existing = jdbc.queryForObject("""
                    SELECT count(*) FROM srm.rfq_invitation WHERE rfq_id = ? AND supplier_id = ?
                    """, Integer.class, rfqId, supplierId);
            if (existing != null && existing > 0) {
                throw new ResponseStatusException(CONFLICT, "Supplier is already invited to this RFQ");
            }
            jdbc.update("""
                    INSERT INTO srm.rfq_invitation (rfq_id, supplier_id, invitation_status)
                    VALUES (?, ?, 'INVITED')
                    """, rfqId, supplierId);
            jdbc.update("""
                    INSERT INTO srm.audit_event
                        (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id, rfq_id,
                         outcome, changed_fields)
                    VALUES (?, 'PROCUREMENT_BUYER', 'RFQ_SUPPLIER_INVITED', 'RFQ_INVITATION',
                            ?, ?, ?, 'SUCCESS', jsonb_build_object('rfq_number', ?, 'invitation_status', 'INVITED'))
                    """, buyerId, rfqId, supplierId, rfqId, header.rfqNumber());
        }
        return jdbc.query("""
                SELECT i.supplier_id, s.supplier_number, s.legal_name, i.invitation_status, i.invited_at
                FROM srm.rfq_invitation i
                JOIN srm.supplier_organization s ON s.supplier_id = i.supplier_id
                WHERE i.rfq_id = ? ORDER BY i.invited_at, s.legal_name
                """, this::mapInvitation, rfqId);
    }

    @Transactional
    public RfqResponse publish(UUID rfqId, UUID buyerId) {
        RfqHeader header = findHeader(rfqId, buyerId, true);
        if (!"DRAFT".equals(header.status())) {
            throw new ResponseStatusException(CONFLICT, "Only draft RFQs can be published");
        }
        if (!header.submissionDeadline().isAfter(Instant.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "Submission deadline must be in the future");
        }
        Integer lineCount = jdbc.queryForObject("SELECT count(*) FROM srm.rfq_line WHERE rfq_id = ?",
                Integer.class, rfqId);
        Integer invitationCount = jdbc.queryForObject("SELECT count(*) FROM srm.rfq_invitation WHERE rfq_id = ?",
                Integer.class, rfqId);
        if (lineCount == null || lineCount < 1) {
            throw new ResponseStatusException(BAD_REQUEST, "Add at least one line before publishing");
        }
        if (invitationCount == null || invitationCount < 1) {
            throw new ResponseStatusException(BAD_REQUEST, "Invite at least one approved supplier before publishing");
        }
        int updated = jdbc.update("""
                UPDATE srm.rfq SET status = 'PUBLISHED', updated_at = now()
                WHERE rfq_id = ? AND buyer_user_id = ? AND status = 'DRAFT'
                """, rfqId, buyerId);
        if (updated != 1) {
            throw new ResponseStatusException(CONFLICT, "RFQ status changed; refresh and retry");
        }
        jdbc.update("""
                INSERT INTO srm.notification
                    (recipient_user_id, event_type, channel, delivery_status, title, body, target_path)
                SELECT m.user_id, 'RFQ_INVITATION', channels.channel, 'QUEUED',
                       'New RFQ invitation ' || r.rfq_number,
                       'Your organization has been invited to ' || r.title || '. Submission deadline: '
                       || to_char(r.submission_deadline AT TIME ZONE 'UTC', 'YYYY-MM-DD HH24:MI') || ' UTC.',
                       '/supplier-portal/quotations'
                FROM srm.rfq r
                JOIN srm.rfq_invitation i ON i.rfq_id = r.rfq_id
                JOIN srm.supplier_user_membership m ON m.supplier_id = i.supplier_id
                CROSS JOIN (VALUES ('IN_APP'), ('EMAIL')) AS channels(channel)
                WHERE r.rfq_id = ? AND i.invitation_status IN ('INVITED', 'ACKNOWLEDGED')
                  AND m.membership_status = 'ACTIVE'
                """, rfqId);
        writeAudit(buyerId, "RFQ_PUBLISHED", rfqId, null, header.rfqNumber(), "PUBLISHED");
        return findOne(rfqId, buyerId, true);
    }

    private void insertLine(UUID rfqId, int lineNumber, RfqLineRequest line) {
        jdbc.update("""
                INSERT INTO srm.rfq_line
                    (rfq_id, line_number, sap_material_code, description, drawing_revision,
                     quantity, uom_code, plant_code, technical_specifications)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, rfqId, lineNumber, line.sapMaterialCode().trim(), line.description().trim(),
                blankToNull(line.drawingRevision()), line.quantity(), line.uomCode().trim().toUpperCase(),
                blankToNull(line.plantCode()), blankToNull(line.technicalSpecifications()));
    }

    private RfqHeader findHeader(UUID rfqId, UUID actorId, boolean restrictToBuyer) {
        List<RfqHeader> results = jdbc.query("""
                SELECT rfq_id, rfq_number, title, description, project_code, department,
                       plant_code, purchasing_group, currency_code, submission_deadline,
                       required_delivery_date, payment_terms, incoterms, status, created_at
                FROM srm.rfq
                WHERE rfq_id = ? AND (? = false OR buyer_user_id = ?)
                """, (rs, row) -> new RfqHeader(
                rs.getObject("rfq_id", UUID.class), rs.getString("rfq_number"), rs.getString("title"),
                rs.getString("description"), rs.getString("project_code"), rs.getString("department"),
                rs.getString("plant_code"), rs.getString("purchasing_group"), rs.getString("currency_code"),
                rs.getTimestamp("submission_deadline").toInstant(),
                rs.getObject("required_delivery_date", LocalDate.class), rs.getString("payment_terms"),
                rs.getString("incoterms"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()
        ), rfqId, restrictToBuyer, actorId);
        if (results.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "RFQ not found");
        }
        return results.getFirst();
    }

    private void writeAudit(UUID actorId, String action, UUID rfqId, UUID supplierId,
                            String rfqNumber, String status) {
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id, rfq_id,
                     outcome, changed_fields)
                VALUES (?, 'PROCUREMENT_BUYER', ?, 'RFQ', ?, ?, ?, 'SUCCESS',
                        jsonb_build_object('rfq_number', ?, 'status', ?))
                """, actorId, action, rfqId, supplierId, rfqId, rfqNumber, status);
    }

    private RfqLineResponse mapLine(ResultSet rs, int row) throws SQLException {
        return new RfqLineResponse(rs.getObject("rfq_line_id", UUID.class), rs.getInt("line_number"),
                rs.getString("sap_material_code"), rs.getString("description"), rs.getString("drawing_revision"),
                rs.getBigDecimal("quantity"), rs.getString("uom_code"), rs.getString("plant_code"),
                rs.getString("technical_specifications"));
    }

    private RfqInvitationResponse mapInvitation(ResultSet rs, int row) throws SQLException {
        return new RfqInvitationResponse(rs.getObject("supplier_id", UUID.class),
                rs.getString("supplier_number"), rs.getString("legal_name"), rs.getString("invitation_status"),
                rs.getTimestamp("invited_at").toInstant());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record RfqHeader(
            UUID rfqId, String rfqNumber, String title, String description, String projectCode,
            String department, String plantCode, String purchasingGroup, String currencyCode,
            Instant submissionDeadline, LocalDate requiredDeliveryDate, String paymentTerms,
            String incoterms, String status, Instant createdAt
    ) {}
}
