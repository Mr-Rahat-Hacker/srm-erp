export interface ApiResponse<T> { success: boolean; message: string; data: T; timestamp: string; }
export interface NegotiationMessage {
  messageId: string;
  senderUserId: string;
  senderName: string;
  senderRole: string;
  messageType: string;
  messageText: string;
  quotationVersionId: string | null;
  quotationVersionNumber: number | null;
  createdAt: string;
}
export interface NegotiationRound {
  negotiationRoundId: string;
  rfqId: string;
  rfqNumber: string;
  rfqTitle: string;
  supplierId: string;
  supplierNumber: string | null;
  supplierName: string;
  roundNumber: number;
  status: string;
  openedAt: string;
  responseDeadline: string | null;
  closedAt: string | null;
  outcome: string | null;
  outcomeRemarks: string | null;
  baselineQuotationVersion: number | null;
  messages: NegotiationMessage[];
}
export interface NegotiationCreatePayload {
  supplierIds: string[];
  responseDeadline: string;
  initialRequest: string;
}
export interface NegotiationReplyPayload {
  message: string;
  quotationVersionId: string | null;
}
export interface NegotiationClosePayload {
  outcome: string;
  remarks: string;
}
