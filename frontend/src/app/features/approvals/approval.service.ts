import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, ApprovalReviewContext, ApprovalTask, AwardRecord, EligibleApprover } from './approval.models';

@Injectable({ providedIn: 'root' })
export class ApprovalService {
  private readonly http = inject(HttpClient);

  getEligibleApprovers(): Observable<ApiResponse<EligibleApprover[]>> {
    return this.http.get<ApiResponse<EligibleApprover[]>>('/api/v1/approvals/eligible-approvers');
  }

  getRfqRequests(rfqId: string): Observable<ApiResponse<ApprovalTask[]>> {
    return this.http.get<ApiResponse<ApprovalTask[]>>(`/api/v1/rfqs/${rfqId}/award-approvals`);
  }

  getAwardForRfq(rfqId: string): Observable<ApiResponse<AwardRecord | null>> {
    return this.http.get<ApiResponse<AwardRecord | null>>(`/api/v1/rfqs/${rfqId}/award`);
  }

  requestApproval(rfqId: string, recommendationId: string): Observable<ApiResponse<ApprovalTask>> {
    return this.http.post<ApiResponse<ApprovalTask>>(`/api/v1/rfqs/${rfqId}/award-approvals`,
      { recommendationId });
  }

  award(rfqId: string, approvalRequestId: string): Observable<ApiResponse<AwardRecord>> {
    return this.http.post<ApiResponse<AwardRecord>>(
      `/api/v1/rfqs/${rfqId}/award?approvalRequestId=${encodeURIComponent(approvalRequestId)}`, {},
    );
  }

  getMyTasks(): Observable<ApiResponse<ApprovalTask[]>> {
    return this.http.get<ApiResponse<ApprovalTask[]>>('/api/v1/approvals/my-tasks');
  }

  getReviewContext(taskId: string): Observable<ApiResponse<ApprovalReviewContext>> {
    return this.http.get<ApiResponse<ApprovalReviewContext>>(
      `/api/v1/approvals/tasks/${taskId}/review-context`,
    );
  }

  decide(taskId: string, decision: 'APPROVE' | 'REJECT' | 'RETURN_FOR_REVISION', comments: string): Observable<ApiResponse<ApprovalTask>> {
    return this.http.post<ApiResponse<ApprovalTask>>(
      `/api/v1/approvals/tasks/${taskId}/decision`, { decision, comments },
    );
  }
}
