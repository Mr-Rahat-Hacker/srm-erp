import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Rfq,
  RfqApiResponse,
  RfqCreatePayload,
  RfqInvitation,
  RfqSummary,
} from './rfq.models';

const API_URL = '/api/v1/rfqs';

@Injectable({ providedIn: 'root' })
export class RfqService {
  private readonly http = inject(HttpClient);

  getRfqs(page = 0, size = 25, q = '', status = ''): Observable<RfqApiResponse<{ items: RfqSummary[]; page: number; size: number; totalElements: number; totalPages: number }>> {
    const params = new URLSearchParams({ page: String(page), size: String(size), q, status });
    return this.http.get<RfqApiResponse<{ items: RfqSummary[]; page: number; size: number; totalElements: number; totalPages: number }>>(`${API_URL}/page?${params}`);
  }

  getRfq(rfqId: string): Observable<RfqApiResponse<Rfq>> {
    return this.http.get<RfqApiResponse<Rfq>>(`${API_URL}/${rfqId}`);
  }

  createRfq(payload: RfqCreatePayload): Observable<RfqApiResponse<Rfq>> {
    return this.http.post<RfqApiResponse<Rfq>>(API_URL, payload);
  }

  inviteSuppliers(rfqId: string, supplierIds: string[]): Observable<RfqApiResponse<RfqInvitation[]>> {
    return this.http.post<RfqApiResponse<RfqInvitation[]>>(
      `${API_URL}/${rfqId}/invitations`,
      { supplierIds },
    );
  }

  publishRfq(rfqId: string): Observable<RfqApiResponse<Rfq>> {
    return this.http.post<RfqApiResponse<Rfq>>(`${API_URL}/${rfqId}/publish`, {});
  }
}
