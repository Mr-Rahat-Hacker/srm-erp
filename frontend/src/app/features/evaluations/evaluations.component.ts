import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { QuotationComparison, ComparisonSupplier, RfqSummary } from '../quotations/comparison.models';
import { QuotationComparisonService } from '../quotations/quotation-comparison.service';
import { EvaluationCreatePayload, RfqEvaluationHistory } from './evaluation.models';
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

  protected rfqs: RfqSummary[] = [];
  protected selectedRfqId = '';
  protected comparison: QuotationComparison | null = null;
  protected history: RfqEvaluationHistory | null = null;
  protected drafts: Record<string, EvaluationDraft> = {};
  protected loading = false;
  protected savingSupplierId = '';
  protected errorMessage = '';
  protected successMessage = '';

  protected get submittedSuppliers(): ComparisonSupplier[] {
    return (this.comparison?.suppliers ?? []).filter((supplier) => supplier.submittedVersion !== null);
  }

  ngOnInit(): void {
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
