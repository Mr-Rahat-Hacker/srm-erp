import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, ReportsDashboard } from './reports.models';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);

  dashboard(): Observable<ApiResponse<ReportsDashboard>> {
    return this.http.get<ApiResponse<ReportsDashboard>>('/api/v1/reports/dashboard');
  }
}
