import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, AuditEvent, AuditSearchFilters } from './audit.models';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);

  search(filters: AuditSearchFilters): Observable<ApiResponse<AuditEvent[]>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== null && value !== '') params = params.set(key, String(value));
    }
    return this.http.get<ApiResponse<AuditEvent[]>>('/api/v1/audit-events', { params });
  }
}
