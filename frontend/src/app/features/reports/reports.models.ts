export interface ApiResponse<T> { success: boolean; message: string; data: T; timestamp: string; }

export interface ReportsDashboard {
  audience: 'INTERNAL' | 'SUPPLIER';
  supplierInvitations: number;
  activeRfqs: number;
  submittedQuotations: number;
  openNegotiations: number;
  pendingApprovals: number;
  awards: number;
  supplierStatuses: Record<string, number>;
  rfqStatuses: Record<string, number>;
}
