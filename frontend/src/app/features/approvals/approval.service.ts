import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, ApprovalTask } from './approval.models';

@Injectable({ providedIn: 'root' })
export class ApprovalService {
  private readonly http = inject(HttpClient);

  getMyTasks(): Observable<ApiResponse<ApprovalTask[]>> {
    return this.http.get<ApiResponse<ApprovalTask[]>>('/api/v1/approvals/my-tasks');
  }

  decide(taskId: string, decision: 'APPROVE' | 'REJECT' | 'RETURN_FOR_REVISION', comments: string): Observable<ApiResponse<ApprovalTask>> {
    return this.http.post<ApiResponse<ApprovalTask>>(
      `/api/v1/approvals/tasks/${taskId}/decision`, { decision, comments },
    );
  }
}
