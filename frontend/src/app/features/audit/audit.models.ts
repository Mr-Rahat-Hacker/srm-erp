export interface ApiResponse<T> { success: boolean; message: string; data: T; timestamp: string; }

export interface AuditEvent {
  eventId: string;
  occurredAt: string;
  actorUserId: string | null;
  actorName: string | null;
  effectiveRoles: string | null;
  actionCode: string;
  entityType: string;
  entityId: string | null;
  supplierId: string | null;
  supplierName: string | null;
  rfqId: string | null;
  rfqNumber: string | null;
  outcome: string;
  reason: string | null;
  changedFields: string | null;
}

export interface AuditSearchFilters {
  action?: string;
  entityType?: string;
  rfqId?: string;
  actorUserId?: string;
  from?: string;
  to?: string;
  limit?: number;
}
