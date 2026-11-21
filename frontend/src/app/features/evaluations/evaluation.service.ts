import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, EvaluationCreatePayload, RfqEvaluationHistory } from './evaluation.models';

@Injectable({ providedIn: 'root' })
export class EvaluationService {
  private readonly http = inject(HttpClient);

  getHistory(rfqId: string): Observable<ApiResponse<RfqEvaluationHistory>> {
    return this.http.get<ApiResponse<RfqEvaluationHistory>>(`/api/v1/rfqs/${rfqId}/evaluations`);
  }

  save(rfqId: string, payload: EvaluationCreatePayload): Observable<ApiResponse<RfqEvaluationHistory>> {
    return this.http.post<ApiResponse<RfqEvaluationHistory>>(
      `/api/v1/rfqs/${rfqId}/evaluations`, payload,
    );
  }
}
