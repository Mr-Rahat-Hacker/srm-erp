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
