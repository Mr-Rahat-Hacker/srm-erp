import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { QuotationComparison, ComparisonSupplier, RfqSummary } from '../quotations/comparison.models';
import { QuotationComparisonService } from '../quotations/quotation-comparison.service';
import { NegotiationRound } from './negotiation.models';
import { NegotiationService } from './negotiation.service';

@Component({
  selector: 'app-buyer-negotiations',
  imports: [DatePipe, FormsModule],
  templateUrl: './buyer-negotiations.component.html',
  styleUrl: './negotiations.component.scss',
})
export class BuyerNegotiationsComponent implements OnInit {
  private readonly comparisons = inject(QuotationComparisonService);
  private readonly service = inject(NegotiationService);

  protected rfqs: RfqSummary[] = [];
  protected comparison: QuotationComparison | null = null;
  protected selectedRfqId = '';
  protected rounds: NegotiationRound[] = [];
  protected selectedSupplierIds: string[] = [];
  protected responseDeadline = this.defaultDeadline();
  protected initialRequest = '';
  protected outcomes: Record<string, string> = {};
  protected outcomeRemarks: Record<string, string> = {};
  protected loading = false;
  protected sending = false;
  protected errorMessage = '';
  protected successMessage = '';

  protected get participatingSuppliers(): ComparisonSupplier[] {
    return (this.comparison?.suppliers ?? []).filter((supplier) => supplier.submittedVersion !== null);
  }

  ngOnInit(): void {
    this.comparisons.getRfqs().subscribe({
      next: (response) => { this.rfqs = response.data ?? []; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 403
          ? 'Only an RFQ owner can manage its negotiation rounds.'
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
        this.loadRounds();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load submitted quotations.';
        this.loading = false;
      },
    });
  }

  private loadRounds(): void {
    if (!this.selectedRfqId) return;
    this.service.getBuyerRounds(this.selectedRfqId).subscribe({
      next: (response) => {
        this.rounds = response.data ?? [];
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load negotiation history.';
        this.loading = false;
      },
    });
  }

  protected initiate(): void {
    if (!this.comparison || !this.selectedSupplierIds.length || !this.initialRequest.trim() || this.sending) return;
    this.sending = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.initiate(this.comparison.rfqId, {
      supplierIds: this.selectedSupplierIds,
      responseDeadline: new Date(this.responseDeadline).toISOString(),
      initialRequest: this.initialRequest.trim(),
    }).subscribe({
      next: (response) => {
        this.rounds = response.data ?? [];
        this.initialRequest = '';
        this.selectedSupplierIds = [];
        this.successMessage = 'Negotiation request sent. Each supplier sees only its own round.';
        this.sending = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to start negotiation.';
        this.sending = false;
      },
    });
  }

  protected closeRound(round: NegotiationRound): void {
    const outcome = this.outcomes[round.negotiationRoundId] || 'AGREED';
    const remarks = this.outcomeRemarks[round.negotiationRoundId] || '';
    this.service.close(round.rfqId, round.negotiationRoundId, { outcome, remarks }).subscribe({
      next: (response) => {
        this.rounds = this.rounds.map((item) =>
          item.negotiationRoundId === round.negotiationRoundId ? response.data : item);
        this.successMessage = 'Negotiation round closed and outcome audited.';
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to close this negotiation round.';
      },
    });
  }

  protected hasOpenRound(supplierId: string): boolean {
    return this.rounds.some((round) => round.supplierId === supplierId && round.status === 'OPEN');
  }

  private defaultDeadline(): string {
    const date = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000);
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return local.toISOString().slice(0, 16);
  }
}
