package com.srm.erp.approval.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ApprovalReviewContext(RfqContext rfq, ApprovalTaskRecord approval,
                                    List<QuotationContext> quotations,
                                    List<NegotiationActivity> negotiationHistory,
                                    List<EvaluationContext> evaluations) {
    public record RfqContext(UUID rfqId, String rfqNumber, String title, String description,
                             String currencyCode, Instant submissionDeadline,
                             LocalDate requiredDeliveryDate, String paymentTerms,
                             String incoterms, String plantCode, String purchasingGroup) {}

    public record QuotationContext(UUID supplierId, String supplierNumber, String supplierName,
                                   UUID quotationVersionId, int versionNumber, Instant submittedAt,
                                   String currencyCode, BigDecimal totalLandedCost,
                                   Integer leadTimeDays, LocalDate validityDate, String incoterms,
                                   String technicalDeviations, List<QuotationLineContext> lines) {}

    public record QuotationLineContext(int lineNumber, String materialCode, String description,
                                       BigDecimal quantity, String unit, BigDecimal basicUnitPrice,
                                       BigDecimal netUnitPrice, BigDecimal taxAmount,
                                       BigDecimal freightAmount, BigDecimal landedLineTotal) {}

    public record NegotiationActivity(UUID supplierId, String supplierName, int roundNumber,
                                      String roundStatus, Instant responseDeadline, String outcome,
                                      String outcomeRemarks, String senderName, String messageType,
                                      String messageText, Instant messageCreatedAt,
                                      Integer quotationVersion) {}

    public record EvaluationContext(UUID supplierId, String supplierName, int quotationVersion,
                                    String evaluationType, String result, String evaluatorName,
                                    String remarks, Instant evaluatedAt) {}
}
