import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiResponse, NegotiationClosePayload, NegotiationCreatePayload, NegotiationReplyPayload, NegotiationRound,
} from './negotiation.models';

@Injectable({ providedIn: 'root' })
export class NegotiationService {
  private readonly http = inject(HttpClient);

  getBuyerRounds(rfqId: string): Observable<ApiResponse<NegotiationRound[]>> {
    return this.http.get<ApiResponse<NegotiationRound[]>>(`/api/v1/rfqs/${rfqId}/negotiations`);
  }

  initiate(rfqId: string, request: NegotiationCreatePayload): Observable<ApiResponse<NegotiationRound[]>> {
    return this.http.post<ApiResponse<NegotiationRound[]>>(`/api/v1/rfqs/${rfqId}/negotiations`, request);
  }

  close(rfqId: string, roundId: string, request: NegotiationClosePayload): Observable<ApiResponse<NegotiationRound>> {
    return this.http.post<ApiResponse<NegotiationRound>>(
      `/api/v1/rfqs/${rfqId}/negotiations/${roundId}/close`, request,
    );
  }

  getSupplierRounds(): Observable<ApiResponse<NegotiationRound[]>> {
    return this.http.get<ApiResponse<NegotiationRound[]>>('/api/v1/supplier/negotiations');
  }

  reply(roundId: string, request: NegotiationReplyPayload): Observable<ApiResponse<NegotiationRound>> {
    return this.http.post<ApiResponse<NegotiationRound>>(
      `/api/v1/supplier/negotiations/${roundId}/messages`, request,
    );
  }
}
