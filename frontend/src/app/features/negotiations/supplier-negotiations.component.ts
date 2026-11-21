import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SupplierRfq } from '../quotations/quotation.models';
import { SupplierQuotationsService } from '../quotations/supplier-quotations.service';
import { NegotiationRound } from './negotiation.models';
import { NegotiationService } from './negotiation.service';

@Component({
  selector: 'app-supplier-negotiations',
  imports: [DatePipe, FormsModule],
  templateUrl: './supplier-negotiations.component.html',
  styleUrl: './negotiations.component.scss',
})
export class SupplierNegotiationsComponent implements OnInit {
  private readonly service = inject(NegotiationService);
  private readonly quotations = inject(SupplierQuotationsService);

  protected rounds: NegotiationRound[] = [];
  protected selected: NegotiationRound | null = null;
  protected rfq: SupplierRfq | null = null;
  protected replyText = '';
  protected selectedVersionId = '';
  protected loading = false;
  protected saving = false;
  protected errorMessage = '';
  protected successMessage = '';

  protected get eligibleVersions() {
    return (this.rfq?.quotationVersions ?? []).filter((version) =>
      version.versionStatus === 'SUBMITTED'
      && version.versionNumber > (this.selected?.baselineQuotationVersion ?? 0));
  }

  protected get deadlinePassed(): boolean {
    return !!this.selected?.responseDeadline
      && new Date(this.selected.responseDeadline).getTime() <= Date.now();
  }

  ngOnInit(): void { this.loadRounds(); }

  protected loadRounds(): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.getSupplierRounds().subscribe({
      next: (response) => { this.rounds = response.data ?? []; this.loading = false; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 403 ? 'Supplier negotiation access is required.'
          : error.error?.message ?? 'Unable to load negotiation requests.';
        this.loading = false;
      },
    });
  }

  protected openRound(round: NegotiationRound): void {
    this.selected = round;
    this.replyText = '';
    this.selectedVersionId = '';
    this.rfq = null;
    this.quotations.getRfq(round.rfqId).subscribe({
      next: (response) => { this.rfq = response.data; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load the RFQ quotation history.';
      },
    });
  }

  protected reply(): void {
    if (!this.selected || !this.replyText.trim() || this.saving || this.deadlinePassed) return;
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.reply(this.selected.negotiationRoundId, {
      message: this.replyText.trim(),
      quotationVersionId: this.selectedVersionId || null,
    }).subscribe({
      next: (response) => {
        this.selected = response.data;
        this.rounds = this.rounds.map((round) =>
          round.negotiationRoundId === response.data.negotiationRoundId ? response.data : round);
        this.replyText = '';
        this.successMessage = 'Your response has been recorded in the negotiation history.';
        this.saving = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to send this response.';
        this.saving = false;
      },
    });
  }

  protected close(): void { this.selected = null; this.rfq = null; }
}
