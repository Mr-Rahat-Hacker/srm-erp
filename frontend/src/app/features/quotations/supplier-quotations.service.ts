import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, QuotationDraftPayload, SupplierRfq, SupplierRfqSummary } from './quotation.models';

const API_URL = '/api/v1/supplier';

@Injectable({ providedIn: 'root' })
export class SupplierQuotationsService {
  private readonly http = inject(HttpClient);

  getInvitedRfqs(): Observable<ApiResponse<SupplierRfqSummary[]>> {
    return this.http.get<ApiResponse<SupplierRfqSummary[]>>(`${API_URL}/rfqs`);
  }

  getRfq(rfqId: string): Observable<ApiResponse<SupplierRfq>> {
    return this.http.get<ApiResponse<SupplierRfq>>(`${API_URL}/rfqs/${rfqId}`);
  }

  saveDraft(rfqId: string, draft: QuotationDraftPayload): Observable<ApiResponse<SupplierRfq>> {
    return this.http.put<ApiResponse<SupplierRfq>>(`${API_URL}/rfqs/${rfqId}/quotation`, draft);
  }

  submit(rfqId: string): Observable<ApiResponse<SupplierRfq>> {
    return this.http.post<ApiResponse<SupplierRfq>>(`${API_URL}/rfqs/${rfqId}/quotation/submit`, {});
  }
}
