export interface ApiResponse<T> { success: boolean; message: string; data: T; timestamp: string; }

export interface NotificationRecord {
  notificationId: string;
  eventType: string;
  title: string | null;
  body: string | null;
  targetPath: string | null;
  deliveryStatus: string;
  createdAt: string;
  sentAt: string | null;
  readAt: string | null;
  retryCount: number;
  lastError: string | null;
}
