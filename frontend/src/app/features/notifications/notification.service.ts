import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, NotificationRecord } from './notification.models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);

  inbox(): Observable<ApiResponse<NotificationRecord[]>> {
    return this.http.get<ApiResponse<NotificationRecord[]>>('/api/v1/notifications');
  }

  markRead(notificationId: string): Observable<ApiResponse<NotificationRecord>> {
    return this.http.post<ApiResponse<NotificationRecord>>(
      `/api/v1/notifications/${notificationId}/read`, {},
    );
  }
}
