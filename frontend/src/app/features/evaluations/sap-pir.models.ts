export interface ApiResponse<T> { success: boolean; message: string; data: T; timestamp: string; }

export interface SapIntegrationRecord {
  integrationEventId: string;
  requestKey: string;
  awardId: string;
  operationType: string;
  status: string;
  externalReference: string | null;
  sanitizedError: string | null;
  attemptCount: number;
  createdAt: string;
  updatedAt: string;
}
