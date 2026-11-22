import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { QuotationComparison, ComparisonSupplier, RfqSummary } from '../quotations/comparison.models';
import { QuotationComparisonService } from '../quotations/quotation-comparison.service';
import { EvaluationCreatePayload, RfqEvaluationHistory } from './evaluation.models';
import { ApprovalTask, AwardRecord, EligibleApprover } from '../approvals/approval.models';
import { ApprovalService } from '../approvals/approval.service';
import { SapIntegrationRecord } from './sap-pir.models';
import { SapPirService } from './sap-pir.service';
import { EvaluationService } from './evaluation.service';

interface EvaluationDraft {
  technicalStatus: string;
  technicalRemarks: string;
  commercialRemarks: string;
  recommendSupplier: boolean;
  recommendationJustification: string;
}

@Component({
  selector: 'app-evaluations',
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './evaluations.component.html',
  styleUrl: './evaluations.component.scss',
})
export class EvaluationsComponent implements OnInit {
  private readonly comparisons = inject(QuotationComparisonService);
  private readonly evaluations = inject(EvaluationService);
  private readonly approvals = inject(ApprovalService);
  private readonly sapPir = inject(SapPirService);

  protected rfqs: RfqSummary[] = [];
  protected selectedRfqId = '';
  protected comparison: QuotationComparison | null = null;
  protected history: RfqEvaluationHistory | null = null;
  protected drafts: Record<string, EvaluationDraft> = {};
  protected loading = false;
  protected savingSupplierId = '';
  protected errorMessage = '';
  protected successMessage = '';
  protected eligibleApprovers: EligibleApprover[] = [];
  protected approvalRequests: ApprovalTask[] = [];
  protected selectedApproverToAdd: Record<string, string> = {};
  protected approvalStages: Record<string, string[]> = {};
  protected finalAward: AwardRecord | null = null;
  protected savingWorkflow = false;
  protected sapSystemCode = '';
  protected sapRequests: SapIntegrationRecord[] = [];

  protected get submittedSuppliers(): ComparisonSupplier[] {
    return (this.comparison?.suppliers ?? []).filter((supplier) => supplier.submittedVersion !== null);
  }

  ngOnInit(): void {
    this.approvals.getEligibleApprovers().subscribe({
      next: (response) => { this.eligibleApprovers = response.data ?? []; },
    });
    this.comparisons.getRfqs().subscribe({
      next: (response) => { this.rfqs = response.data ?? []; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 403 ? 'Only an RFQ owner can evaluate its quotations.'
          : error.error?.message ?? 'Unable to load RFQs.';
      },
    });
  }

  protected loadRfq(): void {
    if (!this.selectedRfqId || this.loading) return;
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.approvalRequests = [];
    this.finalAward = null;
    this.comparisons.compare(this.selectedRfqId).subscribe({
      next: (response) => {
        this.comparison = response.data;
        this.loadHistory();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load quotation comparison.';
        this.loading = false;
      },
    });
  }

  private loadHistory(): void {
    if (!this.selectedRfqId) return;
    this.evaluations.getHistory(this.selectedRfqId).subscribe({
      next: (response) => {
        this.history = response.data;
        this.loading = false;
        for (const supplier of this.submittedSuppliers) this.ensureDraft(supplier.supplierId);
        this.loadAwardWorkflow();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load evaluation history.';
        this.loading = false;
      },
    });
  }

  protected draftFor(supplierId: string): EvaluationDraft {
    return this.ensureDraft(supplierId);
  }

  protected saveEvaluation(supplier: ComparisonSupplier): void {
    const draft = this.ensureDraft(supplier.supplierId);
    if (this.savingSupplierId || !draft.technicalStatus || !draft.commercialRemarks.trim()) return;
    if (draft.recommendSupplier && !draft.recommendationJustification.trim()) return;
    this.savingSupplierId = supplier.supplierId;
    this.errorMessage = '';
    this.successMessage = '';
    const payload: EvaluationCreatePayload = {
      supplierId: supplier.supplierId,
      technicalStatus: draft.technicalStatus,
      technicalRemarks: draft.technicalRemarks.trim(),
      commercialRemarks: draft.commercialRemarks.trim(),
      recommendSupplier: draft.recommendSupplier,
      recommendationJustification: draft.recommendationJustification.trim(),
    };
    this.evaluations.save(this.selectedRfqId, payload).subscribe({
      next: (response) => {
        this.history = response.data;
        this.successMessage = draft.recommendSupplier
          ? 'Evaluation and supplier recommendation were recorded in the audit history.'
          : 'Evaluation was recorded in the audit history.';
        this.drafts[supplier.supplierId] = this.emptyDraft();
        this.savingSupplierId = '';
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to save this evaluation.';
        this.savingSupplierId = '';
      },
    });
  }

  private loadAwardWorkflow(): void {
    if (!this.selectedRfqId) return;
    this.approvals.getRfqRequests(this.selectedRfqId).subscribe({
      next: (response) => { this.approvalRequests = response.data ?? []; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load award approval requests.';
      },
    });
    this.approvals.getAwardForRfq(this.selectedRfqId).subscribe({
      next: (response) => { this.finalAward = response.data; if (this.finalAward) this.loadSapRequests(); },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load the final award.';
      },
    });
  }

  protected requestsFor(recommendationId: string): ApprovalTask[] {
    return this.approvalRequests.filter((request) => request.recommendationId === recommendationId);
  }

  private loadSapRequests(): void {
    if (!this.finalAward) return;
    this.sapPir.forAward(this.finalAward.awardId).subscribe({
      next: (response) => { this.sapRequests = response.data ?? []; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load SAP integration status.';
      },
    });
  }

  protected queueSapRequest(): void {
    if (!this.finalAward || !this.sapSystemCode.trim() || this.savingWorkflow) return;
    this.savingWorkflow = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.sapPir.create(this.finalAward.awardId, this.sapSystemCode.trim()).subscribe({
      next: (response) => {
        this.successMessage = response.message;
        this.savingWorkflow = false;
        this.loadSapRequests();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to queue the SAP PIR request.';
        this.savingWorkflow = false;
      },
    });
  }

  protected retrySapRequest(request: SapIntegrationRecord): void {
    if (this.savingWorkflow) return;
    this.savingWorkflow = true;
    this.errorMessage = '';
    this.sapPir.retry(request.integrationEventId).subscribe({
      next: (response) => {
        this.successMessage = response.message;
        this.savingWorkflow = false;
        this.loadSapRequests();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to retry the SAP PIR request.';
        this.savingWorkflow = false;
      },
    });
  }

  protected hasPendingApproval(recommendationId: string): boolean {
    return this.requestsFor(recommendationId).some((request) => request.status === 'PENDING');
  }

  protected stageApproversFor(recommendationId: string): string[] {
    return this.approvalStages[recommendationId] ?? [];
  }

  protected addApprovalStage(recommendationId: string): void {
    const approverId = this.selectedApproverToAdd[recommendationId];
    if (!approverId || this.stageApproversFor(recommendationId).includes(approverId)) return;
    this.approvalStages[recommendationId] = [...this.stageApproversFor(recommendationId), approverId];
    this.selectedApproverToAdd[recommendationId] = '';
  }

  protected removeApprovalStage(recommendationId: string, index: number): void {
    this.approvalStages[recommendationId] =
      this.stageApproversFor(recommendationId).filter((_, stageIndex) => stageIndex !== index);
  }

  protected moveApprovalStage(recommendationId: string, index: number, offset: number): void {
    const stages = [...this.stageApproversFor(recommendationId)];
    const target = index + offset;
    if (target < 0 || target >= stages.length) return;
    [stages[index], stages[target]] = [stages[target], stages[index]];
    this.approvalStages[recommendationId] = stages;
  }

  protected approverName(userId: string): string {
    const approver = this.eligibleApprovers.find((item) => item.userId === userId);
    return approver ? `${approver.fullName} · ${approver.email}` : userId;
  }

  protected requestAwardApproval(recommendationId: string): void {
    const approverIds = this.stageApproversFor(recommendationId);
    if (approverIds.length === 0 || this.savingWorkflow || this.finalAward) return;
    this.savingWorkflow = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.approvals.requestApproval(this.selectedRfqId, recommendationId, approverIds).subscribe({
      next: (response) => {
        this.successMessage = response.message;
        this.approvalStages[recommendationId] = [];
        this.selectedApproverToAdd[recommendationId] = '';
        this.savingWorkflow = false;
        this.loadAwardWorkflow();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to request award approval.';
        this.savingWorkflow = false;
      },
    });
  }

  protected finalizeAward(request: ApprovalTask): void {
    if (this.savingWorkflow || this.finalAward) return;
    this.savingWorkflow = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.approvals.award(this.selectedRfqId, request.approvalRequestId).subscribe({
      next: (response) => {
        this.finalAward = response.data;
        this.successMessage = response.message;
        this.loadSapRequests();
        this.savingWorkflow = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to record the final award.';
        this.savingWorkflow = false;
      },
    });
  }

  protected quoteFor(supplier: ComparisonSupplier, lineId: string): number | null {
    return supplier.lines.find((line) => line.rfqLineId === lineId)?.landedLineTotal ?? null;
  }

  protected technicalHistory(supplierId: string) {
    return (this.history?.technicalEvaluations ?? []).filter((item) => item.supplierId === supplierId);
  }

  protected commercialHistory(supplierId: string) {
    return (this.history?.commercialEvaluations ?? []).filter((item) => item.supplierId === supplierId);
  }

  protected supplierRecommendations(supplierId: string) {
    return (this.history?.recommendations ?? []).filter((item) => item.supplierId === supplierId);
  }

  private ensureDraft(supplierId: string): EvaluationDraft {
    this.drafts[supplierId] ??= this.emptyDraft();
    return this.drafts[supplierId];
  }

  private emptyDraft(): EvaluationDraft {
    return {
      technicalStatus: 'CLARIFICATION_REQUIRED',
      technicalRemarks: '',
      commercialRemarks: '',
      recommendSupplier: false,
      recommendationJustification: '',
    };
  }
}
