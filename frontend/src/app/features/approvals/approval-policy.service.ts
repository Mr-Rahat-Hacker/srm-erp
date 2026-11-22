import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, EligibleApprover } from './approval.models';

export interface ApprovalPolicyStage {
  userId: string;
  fullName: string;
  email: string;
  sequenceNumber: number;
}

export interface ApprovalPolicy {
  policyId: string;
  policyCode: string;
  policyName: string;
  currencyCode: string;
  minimumAmount: number;
  maximumAmount: number | null;
  active: boolean;
  stages: ApprovalPolicyStage[];
  updatedAt: string;
}

export interface ApprovalPolicyPayload {
  policyCode: string;
  policyName: string;
  currencyCode: string;
  minimumAmount: number;
  maximumAmount: number | null;
  approverUserIds: string[];
  reason: string;
}

@Injectable({ providedIn: 'root' })
export class ApprovalPolicyService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/approval-policies';

  findAll(): Observable<ApiResponse<ApprovalPolicy[]>> {
    return this.http.get<ApiResponse<ApprovalPolicy[]>>(this.baseUrl);
  }

  eligibleApprovers(): Observable<ApiResponse<EligibleApprover[]>> {
    return this.http.get<ApiResponse<EligibleApprover[]>>(`${this.baseUrl}/eligible-approvers`);
  }

  create(payload: ApprovalPolicyPayload): Observable<ApiResponse<ApprovalPolicy>> {
    return this.http.post<ApiResponse<ApprovalPolicy>>(this.baseUrl, payload);
  }

  update(policyId: string, payload: ApprovalPolicyPayload): Observable<ApiResponse<ApprovalPolicy>> {
    return this.http.put<ApiResponse<ApprovalPolicy>>(`${this.baseUrl}/${policyId}`, payload);
  }

  deactivate(policyId: string, reason: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.baseUrl}/${policyId}?reason=${encodeURIComponent(reason)}`,
    );
  }
}
