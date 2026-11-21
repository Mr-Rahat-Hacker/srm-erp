package com.srm.erp.negotiation.service;

import com.srm.erp.negotiation.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NegotiationService {
    private final JdbcTemplate jdbc;

    public NegotiationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public List<NegotiationRoundResponse> initiate(UUID rfqId, UUID buyerId, NegotiationCreateRequest request) {
        if (request.responseDeadline() == null || !request.responseDeadline().isAfter(Instant.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "Negotiation response deadline must be in the future");
        }
        if (request.supplierIds() == null || request.supplierIds().isEmpty()
                || new HashSet<>(request.supplierIds()).size() != request.supplierIds().size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Select each participating supplier once");
        }
        requireBuyerOwnsRfq(rfqId, buyerId);
        lockBuyerRfq(rfqId, buyerId);
        for (UUID supplierId : request.supplierIds()) {
            List<Participant> participants = jdbc.query("""
                    SELECT s.supplier_id,
                           (SELECT qv.quotation_version_id
                            FROM srm.quotation q
                            JOIN srm.quotation_version qv ON qv.quotation_id = q.quotation_id
                            WHERE q.rfq_id = i.rfq_id AND q.supplier_id = s.supplier_id
                              AND qv.version_status = 'SUBMITTED'
                            ORDER BY qv.version_number DESC LIMIT 1) AS baseline_version_id
                    FROM srm.rfq_invitation i
                    JOIN srm.supplier_organization s ON s.supplier_id = i.supplier_id
                    WHERE i.rfq_id = ? AND s.supplier_id = ?
                      AND i.invitation_status IN ('INVITED', 'ACKNOWLEDGED')
                      AND s.lifecycle_status IN ('APPROVED', 'ACTIVE')
                    """, (rs, row) -> new Participant(rs.getObject("supplier_id", UUID.class),
                    rs.getObject("baseline_version_id", UUID.class)), rfqId, supplierId);
            if (participants.isEmpty()) {
                throw new ResponseStatusException(NOT_FOUND, "Participating supplier not found");
            }
            Participant participant = participants.getFirst();
            if (participant.baselineVersionId() == null) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "A supplier must submit a quotation before negotiation can start");
            }
            Integer openRounds = jdbc.queryForObject("""
                    SELECT count(*) FROM srm.negotiation_round
                    WHERE rfq_id = ? AND supplier_id = ? AND status = 'OPEN'
                    """, Integer.class, rfqId, supplierId);
            if (openRounds != null && openRounds > 0) {
                throw new ResponseStatusException(CONFLICT, "This supplier already has an open negotiation round");
            }
            Integer roundNumber = jdbc.queryForObject("""
                    SELECT coalesce(max(round_number), 0) + 1 FROM srm.negotiation_round
                    WHERE rfq_id = ? AND supplier_id = ?
                    """, Integer.class, rfqId, supplierId);
            UUID roundId = jdbc.queryForObject("""
                    INSERT INTO srm.negotiation_round
                        (rfq_id, supplier_id, round_number, status, response_deadline,
                         baseline_quotation_version_id)
                    VALUES (?, ?, ?, 'OPEN', ?, ?)
                    RETURNING negotiation_round_id
                    """, UUID.class, rfqId, supplierId, roundNumber, request.responseDeadline(),
                    participant.baselineVersionId());
            insertMessage(roundId, buyerId, "BUYER_REQUEST", request.initialRequest(), null);
            audit(buyerId, supplierId, rfqId, roundId, "NEGOTIATION_ROUND_OPENED");
        }
        return listForBuyer(rfqId, buyerId);
    }

    public List<NegotiationRoundResponse> listForBuyer(UUID rfqId, UUID buyerId) {
        requireBuyerOwnsRfq(rfqId, buyerId);
        return listRounds("r.rfq_id = ? AND f.buyer_user_id = ?", rfqId, buyerId);
    }

    public List<NegotiationRoundResponse> listForSupplier(UUID userId) {
        List<UUID> roundIds = jdbc.query("""
                SELECT r.negotiation_round_id
                FROM srm.negotiation_round r
                JOIN srm.supplier_user_membership m ON m.supplier_id = r.supplier_id
                JOIN srm.supplier_organization s ON s.supplier_id = r.supplier_id
                WHERE m.user_id = ? AND m.membership_status = 'ACTIVE'
                  AND s.lifecycle_status IN ('APPROVED', 'ACTIVE')
                ORDER BY r.opened_at DESC
                """, (rs, row) -> rs.getObject(1, UUID.class), userId);
        List<NegotiationRoundResponse> rounds = new ArrayList<>();
        for (UUID id : roundIds) rounds.add(findRoundForSupplier(id, userId));
        return rounds;
    }

    @Transactional
    public NegotiationRoundResponse reply(UUID roundId, UUID userId, NegotiationMessageRequest request) {
        SupplierRoundLock round = lockSupplierRound(roundId, userId);
        if (!"OPEN".equals(round.status())) {
            throw new ResponseStatusException(CONFLICT, "This negotiation round is closed");
        }
        if (round.responseDeadline() == null || !round.responseDeadline().isAfter(Instant.now())) {
            throw new ResponseStatusException(CONFLICT, "The negotiation response deadline has passed");
        }
        if (request.quotationVersionId() != null) {
            Integer validVersion = jdbc.queryForObject("""
                    SELECT count(*) FROM srm.quotation_version qv
                    JOIN srm.quotation q ON q.quotation_id = qv.quotation_id
                    WHERE qv.quotation_version_id = ? AND qv.version_status = 'SUBMITTED'
                      AND q.rfq_id = ? AND q.supplier_id = ? AND qv.submitted_at >= ?
                      AND qv.version_number > coalesce((
                          SELECT baseline.version_number FROM srm.negotiation_round nr
                          JOIN srm.quotation_version baseline
                            ON baseline.quotation_version_id = nr.baseline_quotation_version_id
                          WHERE nr.negotiation_round_id = ?
                      ), 0)
                    """, Integer.class, request.quotationVersionId(), round.rfqId(), round.supplierId(),
                    round.openedAt(), roundId);
            if (validVersion == null || validVersion != 1) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "Attach only a newly submitted revision for this negotiation");
            }
        }
        insertMessage(roundId, userId, "SUPPLIER_RESPONSE", request.message().trim(),
                request.quotationVersionId());
        audit(userId, round.supplierId(), round.rfqId(), roundId, "NEGOTIATION_SUPPLIER_REPLIED");
        return findRoundForSupplier(roundId, userId);
    }

    @Transactional
    public NegotiationRoundResponse close(UUID rfqId, UUID roundId, UUID buyerId, NegotiationCloseRequest request) {
        requireBuyerOwnsRfq(rfqId, buyerId);
        List<RoundHeader> rounds = jdbc.query("""
                SELECT negotiation_round_id, supplier_id, status
                FROM srm.negotiation_round WHERE negotiation_round_id = ? AND rfq_id = ?
                FOR UPDATE
                """, (rs, row) -> new RoundHeader(
                rs.getObject("negotiation_round_id", UUID.class), rs.getObject("supplier_id", UUID.class),
                rs.getString("status")), roundId, rfqId);
        if (rounds.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Negotiation round not found");
        RoundHeader round = rounds.getFirst();
        if (!"OPEN".equals(round.status())) {
            throw new ResponseStatusException(CONFLICT, "Negotiation round is already closed");
        }
        String outcome = request.outcome().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("AGREED", "NO_AGREEMENT", "WITHDRAWN", "CLOSED").contains(outcome)) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose a supported negotiation outcome");
        }
        int updated = jdbc.update("""
                UPDATE srm.negotiation_round
                SET status = 'CLOSED', outcome = ?, outcome_remarks = ?, closed_at = now()
                WHERE negotiation_round_id = ? AND status = 'OPEN'
                """, outcome, blankToNull(request.remarks()), roundId);
        if (updated != 1) throw new ResponseStatusException(CONFLICT, "Negotiation round changed; refresh and retry");
        audit(buyerId, round.supplierId(), rfqId, roundId, "NEGOTIATION_ROUND_CLOSED");
        return findRoundForBuyer(rfqId, roundId, buyerId);
    }

    private List<NegotiationRoundResponse> listRounds(String scopeClause, UUID rfqId, UUID buyerId) {
        return jdbc.query("""
                SELECT r.negotiation_round_id, r.rfq_id, f.rfq_number, f.title AS rfq_title, r.supplier_id, s.supplier_number, s.legal_name,
                       r.round_number, r.status, r.opened_at, r.response_deadline, r.closed_at,
                       r.outcome, r.outcome_remarks, base.version_number AS baseline_version_number
                FROM srm.negotiation_round r
                JOIN srm.supplier_organization s ON s.supplier_id = r.supplier_id
                JOIN srm.rfq f ON f.rfq_id = r.rfq_id
                LEFT JOIN srm.quotation_version base
                  ON base.quotation_version_id = r.baseline_quotation_version_id
                WHERE """ + scopeClause + " ORDER BY r.opened_at DESC",
                (rs, row) -> roundWithMessages(rs, true), rfqId, buyerId);
    }

    private NegotiationRoundResponse findRoundForSupplier(UUID roundId, UUID userId) {
        List<NegotiationRoundResponse> rounds = jdbc.query("""
                SELECT r.negotiation_round_id, r.rfq_id, r.supplier_id, s.supplier_number, s.legal_name,
                       r.round_number, r.status, r.opened_at, r.response_deadline, r.closed_at,
                       r.outcome, r.outcome_remarks, base.version_number AS baseline_version_number
                FROM srm.negotiation_round r
                JOIN srm.supplier_organization s ON s.supplier_id = r.supplier_id
                JOIN srm.supplier_user_membership m ON m.supplier_id = s.supplier_id
                JOIN srm.rfq f ON f.rfq_id = r.rfq_id
                LEFT JOIN srm.quotation_version base
                  ON base.quotation_version_id = r.baseline_quotation_version_id
                WHERE r.negotiation_round_id = ? AND m.user_id = ? AND m.membership_status = 'ACTIVE'
                  AND s.lifecycle_status IN ('APPROVED', 'ACTIVE')
                """, (rs, row) -> roundWithMessages(rs, false), roundId, userId);
        if (rounds.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Negotiation round not found");
        return rounds.getFirst();
    }

    private NegotiationRoundResponse findRoundForBuyer(UUID rfqId, UUID roundId, UUID buyerId) {
        List<NegotiationRoundResponse> rounds = jdbc.query("""
                SELECT r.negotiation_round_id, r.rfq_id, r.supplier_id, s.supplier_number, s.legal_name,
                       r.round_number, r.status, r.opened_at, r.response_deadline, r.closed_at,
                       r.outcome, r.outcome_remarks, base.version_number AS baseline_version_number
                FROM srm.negotiation_round r
                JOIN srm.supplier_organization s ON s.supplier_id = r.supplier_id
                JOIN srm.rfq f ON f.rfq_id = r.rfq_id
                LEFT JOIN srm.quotation_version base
                  ON base.quotation_version_id = r.baseline_quotation_version_id
                WHERE r.negotiation_round_id = ? AND r.rfq_id = ? AND f.buyer_user_id = ?
                """, (rs, row) -> roundWithMessages(rs, true), roundId, rfqId, buyerId);
        if (rounds.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Negotiation round not found");
        return rounds.getFirst();
    }

    private NegotiationRoundResponse roundWithMessages(java.sql.ResultSet rs, boolean includeIdentity)
            throws java.sql.SQLException {
        UUID roundId = rs.getObject("negotiation_round_id", UUID.class);
        List<NegotiationMessageResponse> messages = jdbc.query("""
                SELECT m.message_id, m.sender_user_id, u.full_name,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM srm.user_role ur
                           JOIN srm.role role_check ON role_check.role_id = ur.role_id
                           WHERE ur.user_id = m.sender_user_id AND role_check.role_code = 'SUPPLIER_USER'
                       ) THEN 'SUPPLIER' ELSE 'BUYER' END AS sender_role,
                       m.message_type, m.message_text, m.quotation_version_id, v.version_number, m.created_at
                FROM srm.negotiation_message m
                JOIN srm.app_user u ON u.user_id = m.sender_user_id

                LEFT JOIN srm.quotation_version v ON v.quotation_version_id = m.quotation_version_id
                WHERE m.negotiation_round_id = ?
                ORDER BY m.created_at, m.message_id
                """, (msg, row) -> new NegotiationMessageResponse(
                msg.getObject("message_id", UUID.class), msg.getObject("sender_user_id", UUID.class),
                msg.getString("full_name"), msg.getString("sender_role"), msg.getString("message_type"),
                msg.getString("message_text"), msg.getObject("quotation_version_id", UUID.class),
                (Integer) msg.getObject("version_number"), msg.getTimestamp("created_at").toInstant()
        ), roundId);
        return new NegotiationRoundResponse(roundId, rs.getObject("rfq_id", UUID.class),
                rs.getString("rfq_number"), rs.getString("rfq_title"), rs.getObject("supplier_id", UUID.class),
                includeIdentity ? rs.getString("supplier_number") : null,
                includeIdentity ? rs.getString("legal_name") : "Your organization",
                rs.getInt("round_number"), rs.getString("status"), rs.getTimestamp("opened_at").toInstant(),
                rs.getTimestamp("response_deadline") == null ? null : rs.getTimestamp("response_deadline").toInstant(),
                rs.getTimestamp("closed_at") == null ? null : rs.getTimestamp("closed_at").toInstant(),
                rs.getString("outcome"), rs.getString("outcome_remarks"),
                (Integer) rs.getObject("baseline_version_number"), messages);
    }

    private void lockBuyerRfq(UUID rfqId, UUID buyerId) {
        jdbc.queryForObject("""
                SELECT rfq_id FROM srm.rfq WHERE rfq_id = ? AND buyer_user_id = ? FOR UPDATE
                """, UUID.class, rfqId, buyerId);
    }

    private SupplierRoundLock lockSupplierRound(UUID roundId, UUID userId) {
        List<SupplierRoundLock> rounds = jdbc.query("""
                SELECT r.status, r.response_deadline, r.rfq_id, r.supplier_id, r.opened_at,
                       base.version_number AS baseline_version_number
                FROM srm.negotiation_round r
                JOIN srm.supplier_organization s ON s.supplier_id = r.supplier_id
                JOIN srm.supplier_user_membership m ON m.supplier_id = s.supplier_id
                LEFT JOIN srm.quotation_version base
                  ON base.quotation_version_id = r.baseline_quotation_version_id
                WHERE r.negotiation_round_id = ? AND m.user_id = ? AND m.membership_status = 'ACTIVE'
                  AND s.lifecycle_status IN ('APPROVED', 'ACTIVE')
                FOR UPDATE OF r
                """, (rs, row) -> new SupplierRoundLock(
                rs.getString("status"),
                rs.getTimestamp("response_deadline") == null ? null : rs.getTimestamp("response_deadline").toInstant(),
                rs.getObject("rfq_id", UUID.class), rs.getObject("supplier_id", UUID.class),
                rs.getTimestamp("opened_at").toInstant(), (Integer) rs.getObject("baseline_version_number")
        ), roundId, userId);
        if (rounds.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Negotiation round not found");
        return rounds.getFirst();
    }

    private void requireBuyerOwnsRfq(UUID rfqId, UUID buyerId) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM srm.rfq WHERE rfq_id = ? AND buyer_user_id = ?
                """, Integer.class, rfqId, buyerId);
        if (count == null || count == 0) throw new ResponseStatusException(NOT_FOUND, "RFQ not found");
    }

    private void insertMessage(UUID roundId, UUID sender, String type, String text, UUID quotationVersionId) {
        jdbc.update("""
                INSERT INTO srm.negotiation_message
                    (negotiation_round_id, sender_user_id, message_type, message_text, quotation_version_id)
                VALUES (?, ?, ?, ?, ?)
                """, roundId, sender, type, text, quotationVersionId);
    }

    private void audit(UUID actor, UUID supplierId, UUID rfqId, UUID roundId, String action) {
        jdbc.update("""
                INSERT INTO srm.audit_event
                    (actor_user_id, actor_type, action_code, entity_type, entity_id, supplier_id, rfq_id,
                     outcome, changed_fields)
                VALUES (?, ?, ?, 'NEGOTIATION_ROUND', ?, ?, ?, 'SUCCESS',
                        jsonb_build_object('action', ?, 'occurred_at', now()))
                """, actor, action.startsWith("NEGOTIATION_SUPPLIER") ? "SUPPLIER_USER" : "PROCUREMENT_BUYER",
                action, roundId, supplierId, rfqId, action);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Participant(UUID supplierId, UUID baselineVersionId) {}
    private record RoundHeader(UUID roundId, UUID supplierId, String status) {}
    private record SupplierRoundLock(String status, Instant responseDeadline, UUID rfqId, UUID supplierId,
                                     Instant openedAt, Integer baselineQuotationVersion) {}
}
