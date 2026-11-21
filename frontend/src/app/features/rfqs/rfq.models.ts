export interface RfqLineDraft {
  sapMaterialCode: string;
  description: string;
  drawingRevision: string;
  quantity: string;
  uomCode: string;
  plantCode: string;
  technicalSpecifications: string;
}

export interface RfqLine {
  rfqLineId: string;
  lineNumber: number;
  sapMaterialCode: string;
  description: string;
  drawingRevision: string | null;
  quantity: number;
  uomCode: string;
  plantCode: string | null;
  technicalSpecifications: string | null;
}

export interface RfqInvitation {
  supplierId: string;
  supplierNumber: string;
  legalName: string;
  invitationStatus: string;
  invitedAt: string;
}

export interface RfqSummary {
  rfqId: string;
  rfqNumber: string;
  title: string;
  status: string;
  currencyCode: string;
  submissionDeadline: string;
  lineCount: number;
  invitationCount: number;
  createdAt: string;
}

export interface Rfq {
  rfqId: string;
  rfqNumber: string;
  title: string;
  description: string | null;
  projectCode: string | null;
  department: string | null;
  plantCode: string | null;
  purchasingGroup: string | null;
  currencyCode: string;
  submissionDeadline: string;
  requiredDeliveryDate: string | null;
  paymentTerms: string | null;
  incoterms: string | null;
  status: string;
  createdAt: string;
  lines: RfqLine[];
  invitations: RfqInvitation[];
}

export interface RfqCreatePayload {
  title: string;
  description: string;
  projectCode: string;
  department: string;
  plantCode: string;
  purchasingGroup: string;
  currencyCode: string;
  submissionDeadline: string;
  requiredDeliveryDate: string | null;
  paymentTerms: string;
  incoterms: string;
  lines: Array<{
    sapMaterialCode: string;
    description: string;
    drawingRevision: string | null;
    quantity: number;
    uomCode: string;
    plantCode: string | null;
    technicalSpecifications: string | null;
  }>;
}

export interface RfqApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
