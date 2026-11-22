import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiResponse, EligibleApprover } from './approval.models';
import {
  ApprovalPolicy,
  ApprovalPolicyPayload,
  ApprovalPolicyService,
} from './approval-policy.service';

@Component({
  selector: 'app-approval-policies',
  imports: [FormsModule],
  templateUrl: './approval-policies.component.html',
  styleUrl: './approval-policies.component.scss',
})
export class ApprovalPoliciesComponent implements OnInit {
  private readonly service = inject(ApprovalPolicyService);

  protected policies: ApprovalPolicy[] = [];
  protected approvers: EligibleApprover[] = [];
  protected loading = false;
  protected saving = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected editingPolicyId = '';
  protected selectedApproverId = '';
  protected deactivateReason = '';
  protected form = this.emptyForm();

  ngOnInit(): void {
    this.load();
    this.service.eligibleApprovers().subscribe({
      next: (response) => { this.approvers = response.data ?? []; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load active procurement approvers.';
      },
    });
  }

  protected load(): void {
    this.loading = true;
    this.service.findAll().subscribe({
      next: (response) => { this.policies = response.data ?? []; this.loading = false; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load approval policies.';
        this.loading = false;
      },
    });
  }

  protected addApprover(): void {
    if (!this.selectedApproverId || this.form.approverUserIds.includes(this.selectedApproverId)
      || this.form.approverUserIds.length >= 10) return;
    this.form.approverUserIds = [...this.form.approverUserIds, this.selectedApproverId];
    this.selectedApproverId = '';
  }

  protected removeApprover(index: number): void {
    this.form.approverUserIds = this.form.approverUserIds.filter((_, stage) => stage !== index);
  }

  protected moveApprover(index: number, offset: number): void {
    const target = index + offset;
    const ids = [...this.form.approverUserIds];
    if (target < 0 || target >= ids.length) return;
    [ids[index], ids[target]] = [ids[target], ids[index]];
    this.form.approverUserIds = ids;
  }

  protected approverName(userId: string): string {
    const user = this.approvers.find((item) => item.userId === userId);
    return user ? `${user.fullName} · ${user.email}` : userId;
  }

  protected edit(policy: ApprovalPolicy): void {
    this.editingPolicyId = policy.policyId;
    this.form = {
      policyCode: policy.policyCode,
      policyName: policy.policyName,
      currencyCode: policy.currencyCode,
      minimumAmount: String(policy.minimumAmount),
      maximumAmount: policy.maximumAmount === null ? '' : String(policy.maximumAmount),
      approverUserIds: policy.stages.map((stage) => stage.userId),
      reason: '',
    };
    this.errorMessage = '';
    this.successMessage = '';
  }

  protected save(): void {
    if (this.saving || !this.form.policyCode.trim() || !this.form.policyName.trim()
      || !this.form.currencyCode.trim() || !this.form.minimumAmount.trim()
      || this.form.approverUserIds.length === 0 || !this.form.reason.trim()) return;
    const minimumAmount = Number(this.form.minimumAmount);
    const maximumAmount = this.form.maximumAmount.trim() ? Number(this.form.maximumAmount) : null;
    if (!Number.isFinite(minimumAmount) || minimumAmount < 0
      || maximumAmount !== null && (!Number.isFinite(maximumAmount) || maximumAmount <= minimumAmount)) {
      this.errorMessage = 'Enter a valid amount range. The maximum must be greater than the minimum.';
      return;
    }
    const payload: ApprovalPolicyPayload = {
      policyCode: this.form.policyCode.trim(),
      policyName: this.form.policyName.trim(),
      currencyCode: this.form.currencyCode.trim().toUpperCase(),
      minimumAmount,
      maximumAmount,
      approverUserIds: [...this.form.approverUserIds],
      reason: this.form.reason.trim(),
    };
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const request: Observable<ApiResponse<ApprovalPolicy>> = this.editingPolicyId
      ? this.service.update(this.editingPolicyId, payload)
      : this.service.create(payload);
    request.subscribe({
      next: (response) => {
        this.successMessage = response.message;
        this.reset();
        this.load();
        this.saving = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to save this approval policy.';
        this.saving = false;
      },
    });
  }

  protected deactivate(policy: ApprovalPolicy): void {
    const reason = this.deactivateReason.trim();
    if (this.saving || !reason) {
      if (!reason) this.errorMessage = 'Add a reason before deactivating a policy.';
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.deactivate(policy.policyId, reason).subscribe({
      next: (response) => {
        this.successMessage = response.message;
        this.deactivateReason = '';
        this.load();
        this.saving = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to deactivate this approval policy.';
        this.saving = false;
      },
    });
  }

  protected reset(): void {
    this.editingPolicyId = '';
    this.selectedApproverId = '';
    this.form = this.emptyForm();
  }

  private emptyForm() {
    return {
      policyCode: '',
      policyName: '',
      currencyCode: 'INR',
      minimumAmount: '0',
      maximumAmount: '',
      approverUserIds: [] as string[],
      reason: '',
    };
  }
}
