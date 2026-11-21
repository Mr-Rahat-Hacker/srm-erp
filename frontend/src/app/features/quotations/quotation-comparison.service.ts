import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, QuotationComparison, RfqSummary } from './comparison.models';

@Injectable({ providedIn: 'root' })
export class QuotationComparisonService {
  private readonly http = inject(HttpClient);

  getRfqs(): Observable<ApiResponse<RfqSummary[]>> {
    return this.http.get<ApiResponse<RfqSummary[]>>('/api/v1/rfqs');
  }

  compare(rfqId: string): Observable<ApiResponse<QuotationComparison>> {
    return this.http.get<ApiResponse<QuotationComparison>>(`/api/v1/rfqs/${rfqId}/quotations`);
  }
}
