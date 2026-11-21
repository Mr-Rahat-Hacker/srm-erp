export interface ApiResponse<T> { success: boolean; message: string; data: T; timestamp: string; }
export interface EvaluationCreatePayload {
  supplierId: string;
  technicalStatus: string;
  technicalRemarks: string;
  commercialRemarks: string;
  recommendSupplier: boolean;
  recommendationJustification: string;
}
export interface TechnicalEvaluationRecord {
  supplierId: string;
  supplierNumber: string;
  supplierName: string;
  quotationVersionId: string;
  quotationVersion: number;
  evaluatorUserId: string;
  evaluatorName: string;
  technicalStatus: string;
  remarks: string | null;
  evaluatedAt: string;
}
export interface CommercialEvaluationRecord {
  supplierId: string;
  supplierNumber: string;
  supplierName: string;
  quotationVersionId: string;
  quotationVersion: number;
  evaluatorUserId: string;
  evaluatorName: string;
  comparisonBasis: string;
  remarks: string;
  evaluatedAt: string;
}
export interface SupplierRecommendationRecord {
  recommendationId: string;
  supplierId: string;
  supplierNumber: string;
  supplierName: string;
  quotationVersionId: string;
  quotationVersion: number;
  recommendedBy: string;
  recommenderName: string;
  nonL1: boolean;
  justification: string;
  createdAt: string;
}
export interface RfqEvaluationHistory {
  rfqId: string;
  rfqNumber: string;
  title: string;
  currencyCode: string;
  technicalEvaluations: TechnicalEvaluationRecord[];
  commercialEvaluations: CommercialEvaluationRecord[];
  recommendations: SupplierRecommendationRecord[];
}
