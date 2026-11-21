import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  QuotationDraftPayload,
  QuotationLineDraft,
  SupplierRfq,
  SupplierRfqSummary,
} from './quotation.models';
import { SupplierQuotationsService } from './supplier-quotations.service';

@Component({
  selector: 'app-supplier-quotations',
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './supplier-quotations.component.html',
  styleUrl: './supplier-quotations.component.scss',
})
export class SupplierQuotationsComponent implements OnInit {
  private readonly service = inject(SupplierQuotationsService);

  protected rfqs: SupplierRfqSummary[] = [];
  protected selected: SupplierRfq | null = null;
  protected lines: QuotationLineDraft[] = [];
  protected loading = false;
  protected saving = false;
  protected submitting = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected form = this.emptyForm();

  protected get deadlinePassed(): boolean {
    return !!this.selected && new Date(this.selected.submissionDeadline).getTime() <= Date.now();
  }

  protected get latestVersion() {
    return this.selected?.quotationVersions[0] ?? null;
  }

  protected get canSubmit(): boolean {
    return !!this.selected && !this.deadlinePassed && !!this.form.validityDate
      && this.form.leadTimeDays !== '' && Number(this.form.leadTimeDays) >= 0
      && this.lines.length === this.selected.lines.length
      && this.lines.every((line) => Number(line.basicUnitPrice) > 0);
  }

  ngOnInit(): void {
    this.loadRfqs();
  }

  protected loadRfqs(): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.getInvitedRfqs().subscribe({
      next: (response) => {
        this.rfqs = response.data ?? [];
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 403
          ? 'This account does not have supplier portal access.'
          : error.error?.message ?? 'Unable to load your invited RFQs.';
        this.loading = false;
      },
    });
  }

  protected openRfq(summary: SupplierRfqSummary): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.service.getRfq(summary.rfqId).subscribe({
      next: (response) => {
        this.selected = response.data;
        this.loadDraft(response.data);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to open this invited RFQ.';
      },
    });
  }

  protected saveDraft(): void {
    if (!this.selected || this.saving || this.deadlinePassed) return;
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const payload: QuotationDraftPayload = {
      currencyCode: this.selected.currencyCode,
      ...this.form,
      validityDate: this.form.validityDate || null,
      leadTimeDays: this.form.leadTimeDays === '' ? null : Number(this.form.leadTimeDays),
      minimumOrderQuantity: this.form.minimumOrderQuantity === '' ? null : Number(this.form.minimumOrderQuantity),
      lines: this.lines.map((line) => ({
        rfqLineId: line.rfqLineId,
        basicUnitPrice: Number(line.basicUnitPrice),
        discountAmount: Number(line.discountAmount || 0),
        taxAmount: Number(line.taxAmount || 0),
        freightAmount: Number(line.freightAmount || 0),
        packingAmount: Number(line.packingAmount || 0),
        toolingAmount: Number(line.toolingAmount || 0),
        insuranceAmount: Number(line.insuranceAmount || 0),
        otherCharges: Number(line.otherCharges || 0),
      })),
    };
    this.service.saveDraft(this.selected.rfqId, payload).subscribe({
      next: (response) => {
        this.selected = response.data;
        this.updateSummary(response.data);
        this.loadDraft(response.data);
        this.successMessage = response.message || 'Quotation draft saved. You can continue editing before submission.';
        this.saving = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to save the quotation draft.';
        this.saving = false;
      },
    });
  }

  protected submitQuotation(): void {
    if (!this.selected || !this.canSubmit || this.submitting) return;
    this.submitting = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.submit(this.selected.rfqId).subscribe({
      next: (response) => {
        this.selected = response.data;
        this.updateSummary(response.data);
        this.loadDraft(response.data);
        this.successMessage = response.message || 'Your quotation version has been submitted.';
        this.submitting = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to submit the quotation.';
        this.submitting = false;
      },
    });
  }

  protected closeDetails(): void {
    this.selected = null;
    this.lines = [];
    this.errorMessage = '';
    this.successMessage = '';
  }

  protected lineDescription(lineId: string): string {
    return this.selected?.lines.find((line) => line.rfqLineId === lineId)?.description ?? '';
  }

  private updateSummary(rfq: SupplierRfq): void {
    const latest = rfq.quotationVersions[0];
    this.rfqs = this.rfqs.map((item) => item.rfqId === rfq.rfqId
      ? { ...item, quotationStatus: latest?.versionStatus ?? null, latestVersion: latest?.versionNumber ?? null }
      : item);
  }

  private loadDraft(rfq: SupplierRfq): void {
    const latest = rfq.quotationVersions[0];
    this.lines = rfq.lines.map((line) => {
      const saved = latest?.lines.find((quoteLine) => quoteLine.rfqLineId === line.rfqLineId);
      return {
        rfqLineId: line.rfqLineId,
        basicUnitPrice: saved?.basicUnitPrice?.toString() ?? '',
        discountAmount: saved?.discountAmount?.toString() ?? '0',
        taxAmount: saved?.taxAmount?.toString() ?? '0',
        freightAmount: saved?.freightAmount?.toString() ?? '0',
        packingAmount: saved?.packingAmount?.toString() ?? '0',
        toolingAmount: saved?.toolingAmount?.toString() ?? '0',
        insuranceAmount: saved?.insuranceAmount?.toString() ?? '0',
        otherCharges: saved?.otherCharges?.toString() ?? '0',
      };
    });
    this.form = latest?.versionStatus === 'DRAFT'
      ? {
        validityDate: latest.validityDate ?? '',
        paymentTerms: latest.paymentTerms ?? '',
        incoterms: latest.incoterms ?? '',
        leadTimeDays: latest.leadTimeDays?.toString() ?? '',
        minimumOrderQuantity: latest.minimumOrderQuantity?.toString() ?? '',
        warrantyTerms: latest.warrantyTerms ?? '',
        commercialRemarks: latest.commercialRemarks ?? '',
        technicalDeviations: latest.technicalDeviations ?? '',
      }
      : this.emptyForm();
  }

  private emptyForm() {
    return {
      validityDate: '',
      paymentTerms: '',
      incoterms: '',
      leadTimeDays: '',
      minimumOrderQuantity: '',
      warrantyTerms: '',
      commercialRemarks: '',
      technicalDeviations: '',
    };
  }
}
