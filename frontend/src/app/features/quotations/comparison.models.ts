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
export interface ComparisonRfqLine {
  rfqLineId: string;
  lineNumber: number;
  sapMaterialCode: string;
  description: string;
  quantity: number;
  uomCode: string;
  plantCode: string | null;
  technicalSpecifications: string | null;
}
export interface ComparisonQuoteLine {
  rfqLineId: string;
  basicUnitPrice: number;
  netUnitPrice: number;
  taxAmount: number;
  freightAmount: number;
  packingAmount: number;
  toolingAmount: number;
  insuranceAmount: number;
  otherCharges: number;
  landedLineTotal: number;
}
export interface ComparisonSupplier {
  supplierId: string;
  supplierNumber: string;
  legalName: string;
  invitationStatus: string;
  submittedVersion: number | null;
  submittedAt: string | null;
  totalLandedCost: number | null;
  leadTimeDays: number | null;
  validityDate: string | null;
  paymentTerms: string | null;
  incoterms: string | null;
  commercialRemarks: string | null;
  technicalDeviations: string | null;
  lines: ComparisonQuoteLine[];
}
export interface QuotationComparison {
  rfqId: string;
  rfqNumber: string;
  title: string;
  currencyCode: string;
  submissionDeadline: string;
  rfqLines: ComparisonRfqLine[];
  suppliers: ComparisonSupplier[];
}
export interface ApiResponse<T> { success: boolean; message: string; data: T; timestamp: string; }
