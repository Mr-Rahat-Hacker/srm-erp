export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
export interface SupplierRfqSummary {
  rfqId: string;
  rfqNumber: string;
  title: string;
  currencyCode: string;
  submissionDeadline: string;
  invitationStatus: string;
  quotationStatus: string | null;
  latestVersion: number | null;
}
export interface SupplierRfqLine {
  rfqLineId: string;
  lineNumber: number;
  sapMaterialCode: string;
  description: string;
  quantity: number;
  uomCode: string;
  plantCode: string | null;
  technicalSpecifications: string | null;
}
export interface QuotationLineDraft {
  rfqLineId: string;
  basicUnitPrice: string;
  discountAmount: string;
  taxAmount: string;
  freightAmount: string;
  packingAmount: string;
  toolingAmount: string;
  insuranceAmount: string;
  otherCharges: string;
}
export interface QuotationLine {
  rfqLineId: string;
  basicUnitPrice: number;
  discountAmount: number;
  netUnitPrice: number;
  taxAmount: number;
  freightAmount: number;
  packingAmount: number;
  toolingAmount: number;
  insuranceAmount: number;
  otherCharges: number;
  landedLineTotal: number;
}
export interface QuotationVersion {
  quotationVersionId: string;
  versionNumber: number;
  versionStatus: string;
  currencyCode: string;
  validityDate: string | null;
  paymentTerms: string | null;
  incoterms: string | null;
  leadTimeDays: number | null;
  minimumOrderQuantity: number | null;
  warrantyTerms: string | null;
  commercialRemarks: string | null;
  technicalDeviations: string | null;
  totalLandedCost: number | null;
  submittedAt: string | null;
  lines: QuotationLine[];
}
export interface SupplierRfq {
  rfqId: string;
  rfqNumber: string;
  title: string;
  description: string | null;
  currencyCode: string;
  submissionDeadline: string;
  requiredDeliveryDate: string | null;
  paymentTerms: string | null;
  incoterms: string | null;
  invitationStatus: string;
  lines: SupplierRfqLine[];
  quotationVersions: QuotationVersion[];
}
export interface QuotationDraftPayload {
  currencyCode: string;
  validityDate: string | null;
  paymentTerms: string;
  incoterms: string;
  leadTimeDays: number | null;
  minimumOrderQuantity: number | null;
  warrantyTerms: string;
  commercialRemarks: string;
  technicalDeviations: string;
  lines: Array<{
    rfqLineId: string;
    basicUnitPrice: number;
    discountAmount: number;
    taxAmount: number;
    freightAmount: number;
    packingAmount: number;
    toolingAmount: number;
    insuranceAmount: number;
    otherCharges: number;
  }>;
}
