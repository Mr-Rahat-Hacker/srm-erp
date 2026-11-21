package com.srm.erp.quotation.service;

import com.srm.erp.quotation.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class QuotationComparisonService {
    private final JdbcTemplate jdbc;

    public QuotationComparisonService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public QuotationComparisonResponse compare(UUID rfqId, UUID buyerId) {
        List<RfqHeader> headers = jdbc.query("""
                SELECT rfq_id, rfq_number, title, currency_code, submission_deadline
                FROM srm.rfq WHERE rfq_id = ? AND buyer_user_id = ?
                """, (rs, row) -> new RfqHeader(
                rs.getObject("rfq_id", UUID.class), rs.getString("rfq_number"), rs.getString("title"),
                rs.getString("currency_code"), rs.getTimestamp("submission_deadline").toInstant()
        ), rfqId, buyerId);
        if (headers.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "RFQ not found");
        }
        RfqHeader header = headers.getFirst();
        List<SupplierRfqLine> rfqLines = jdbc.query("""
                SELECT rfq_line_id, line_number, sap_material_code, description,
                       quantity, uom_code, plant_code, technical_specifications
                FROM srm.rfq_line WHERE rfq_id = ? ORDER BY line_number
                """, (rs, row) -> new SupplierRfqLine(
                rs.getObject("rfq_line_id", UUID.class), rs.getInt("line_number"),
                rs.getString("sap_material_code"), rs.getString("description"),
                rs.getBigDecimal("quantity"), rs.getString("uom_code"), rs.getString("plant_code"),
                rs.getString("technical_specifications")
        ), rfqId);

        List<Invitee> invitees = jdbc.query("""
                SELECT i.supplier_id, s.supplier_number, s.legal_name, i.invitation_status,
                       v.quotation_version_id, v.version_number, v.submitted_at, v.total_landed_cost,
                       v.lead_time_days, v.validity_date, v.payment_terms, v.incoterms,
                       v.commercial_remarks, v.technical_deviations
                FROM srm.rfq_invitation i
                JOIN srm.supplier_organization s ON s.supplier_id = i.supplier_id
                LEFT JOIN LATERAL (
                    SELECT qv.quotation_version_id, qv.version_number, qv.submitted_at,
                           qv.total_landed_cost, qv.lead_time_days, qv.validity_date,
                           qv.payment_terms, qv.incoterms, qv.commercial_remarks, qv.technical_deviations
                    FROM srm.quotation q
                    JOIN srm.quotation_version qv ON qv.quotation_id = q.quotation_id
                    WHERE q.rfq_id = i.rfq_id AND q.supplier_id = i.supplier_id
                      AND qv.version_status = 'SUBMITTED'
                    ORDER BY qv.version_number DESC LIMIT 1
                ) v ON true
                WHERE i.rfq_id = ?
                ORDER BY s.legal_name
                """, (rs, row) -> new Invitee(
                rs.getObject("supplier_id", UUID.class), rs.getString("supplier_number"),
                rs.getString("legal_name"), rs.getString("invitation_status"),
                rs.getObject("quotation_version_id", UUID.class),
                (Integer) rs.getObject("version_number"),
                rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toInstant(),
                rs.getBigDecimal("total_landed_cost"), (Integer) rs.getObject("lead_time_days"),
                rs.getObject("validity_date", LocalDate.class), rs.getString("payment_terms"),
                rs.getString("incoterms"), rs.getString("commercial_remarks"),
                rs.getString("technical_deviations")
        ), rfqId);

        List<QuotationComparisonSupplier> comparisons = invitees.stream().map(invitee -> {
            List<QuotationComparisonLine> quoteLines = invitee.versionId() == null ? List.of()
                    : jdbc.query("""
                    SELECT rfq_line_id, basic_unit_price, net_unit_price, tax_amount, freight_amount,
                           packing_amount, tooling_amount, insurance_amount, other_charges, landed_line_total
                    FROM srm.quotation_line WHERE quotation_version_id = ? ORDER BY rfq_line_id
                    """, (rs, row) -> new QuotationComparisonLine(
                    rs.getObject("rfq_line_id", UUID.class), rs.getBigDecimal("basic_unit_price"),
                    rs.getBigDecimal("net_unit_price"), rs.getBigDecimal("tax_amount"),
                    rs.getBigDecimal("freight_amount"), rs.getBigDecimal("packing_amount"),
                    rs.getBigDecimal("tooling_amount"), rs.getBigDecimal("insurance_amount"),
                    rs.getBigDecimal("other_charges"), rs.getBigDecimal("landed_line_total")
            ), invitee.versionId());
            return new QuotationComparisonSupplier(invitee.supplierId(), invitee.supplierNumber(),
                    invitee.legalName(), invitee.invitationStatus(), invitee.versionNumber(),
                    invitee.submittedAt(), invitee.totalLandedCost(), invitee.leadTimeDays(),
                    invitee.validityDate(), invitee.paymentTerms(), invitee.incoterms(),
                    invitee.commercialRemarks(), invitee.technicalDeviations(), quoteLines);
        }).toList();

        return new QuotationComparisonResponse(header.rfqId(), header.rfqNumber(), header.title(),
                header.currencyCode(), header.deadline(), rfqLines, comparisons);
    }

    private record RfqHeader(UUID rfqId, String rfqNumber, String title, String currencyCode, Instant deadline) {}
    private record Invitee(UUID supplierId, String supplierNumber, String legalName, String invitationStatus,
                           UUID versionId, Integer versionNumber, Instant submittedAt,
                           java.math.BigDecimal totalLandedCost, Integer leadTimeDays, LocalDate validityDate,
                           String paymentTerms, String incoterms, String commercialRemarks, String technicalDeviations) {}
}
