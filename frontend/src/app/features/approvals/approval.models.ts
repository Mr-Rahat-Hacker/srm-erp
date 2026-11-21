export interface ApiResponse<T> { success: boolean; message: string; data: T; timestamp: string; }

export interface ApprovalTask {
  approvalRequestId: string;
  approvalTaskId: string;
  rfqId: string;
  rfqNumber: string;
  rfqTitle: string;
  recommendationId: string;
  supplierId: string;
  supplierNumber: string;
  supplierName: string;
  quotationVersionId: string;
  versionNumber: number;
  justification: string;
  requestedBy: string;
  requesterName: string;
  status: string;
  createdAt: string;
  dueAt: string | null;
}

export interface EligibleApprover {
  userId: string;
  fullName: string;
  email: string;
}

export interface AwardRecord {
  awardId: string;
  rfqId: string;
  rfqNumber: string;
  supplierId: string;
  supplierName: string;
  quotationVersionId: string;
  versionNumber: number;
  approvalRequestId: string;
  approvedValue: number;
  currencyCode: string;
  status: string;
  awardedAt: string;
}

export interface ApprovalReviewContext {
  rfq: {
    rfqId: string; rfqNumber: string; title: string; description: string | null;
    currencyCode: string; submissionDeadline: string; requiredDeliveryDate: string | null;
    paymentTerms: string | null; incoterms: string | null; plantCode: string | null;
    purchasingGroup: string | null;
  };
  approval: ApprovalTask;
  quotations: Array<{
    supplierId: string; supplierNumber: string; supplierName: string;
    quotationVersionId: string; versionNumber: number; submittedAt: string | null;
    currencyCode: string; totalLandedCost: number | null; leadTimeDays: number | null;
    validityDate: string | null; incoterms: string | null; technicalDeviations: string | null;
    lines: Array<{
      lineNumber: number; materialCode: string; description: string; quantity: number; unit: string;
      basicUnitPrice: number; netUnitPrice: number; taxAmount: number;
      freightAmount: number; landedLineTotal: number;
    }>;
  }>;
  negotiationHistory: Array<{
    supplierId: string; supplierName: string; roundNumber: number; roundStatus: string;
    responseDeadline: string | null; outcome: string | null; outcomeRemarks: string | null;
    senderName: string | null; messageType: string | null; messageText: string | null;
    messageCreatedAt: string | null; quotationVersion: number | null;
  }>;
  evaluations: Array<{
    supplierId: string; supplierName: string; quotationVersion: number;
    evaluationType: string; result: string; evaluatorName: string;
    remarks: string | null; evaluatedAt: string;
  }>;
}
