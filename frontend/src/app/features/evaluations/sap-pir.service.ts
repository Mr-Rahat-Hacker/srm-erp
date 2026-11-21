import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, SapIntegrationRecord } from './sap-pir.models';

@Injectable({ providedIn: 'root' })
export class SapPirService {
  private readonly http = inject(HttpClient);

  forAward(awardId: string): Observable<ApiResponse<SapIntegrationRecord[]>> {
    return this.http.get<ApiResponse<SapIntegrationRecord[]>>(`/api/v1/awards/${awardId}/sap-pir`);
  }

  create(awardId: string, sapSystemCode: string): Observable<ApiResponse<SapIntegrationRecord>> {
    return this.http.post<ApiResponse<SapIntegrationRecord>>(`/api/v1/awards/${awardId}/sap-pir`, { sapSystemCode });
  }

  retry(eventId: string): Observable<ApiResponse<SapIntegrationRecord>> {
    return this.http.post<ApiResponse<SapIntegrationRecord>>(
      `/api/v1/sap-integration-events/${eventId}/retry`, {},
    );
  }
}
