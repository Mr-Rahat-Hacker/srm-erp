import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SupplierService } from '../suppliers/supplier.service';
import { Supplier } from '../suppliers/supplier.models';
import { Rfq, RfqCreatePayload, RfqLineDraft, RfqSummary } from './rfq.models';
import { RfqService } from './rfq.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-rfqs',
  imports: [DatePipe, FormsModule],
  templateUrl: './rfqs.component.html',
  styleUrl: './rfqs.component.scss',
})
export class RfqsComponent implements OnInit {
  private readonly rfqService = inject(RfqService);
  private readonly supplierService = inject(SupplierService);
  private readonly auth = inject(AuthService);

  protected get canManageRfqs(): boolean {
    return (this.auth.getUser()?.roles ?? []).includes('PROCUREMENT_BUYER');
  }

  protected rfqs: RfqSummary[] = [];
  protected page = 0;
  protected pageSize = 25;
  protected totalElements = 0;
  protected totalPages = 0;
  protected searchText = '';
  protected statusFilter = '';
  protected suppliers: Supplier[] = [];
  protected selectedRfq: Rfq | null = null;
  protected selectedSupplierIds: string[] = [];
  protected lines: RfqLineDraft[] = [this.emptyLine()];
  protected loading = false;
  protected supplierLoading = false;
  protected creating = false;
  protected inviting = false;
  protected publishing = false;
  protected showCreateForm = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected form = this.emptyForm();

  protected get eligibleSuppliers(): Supplier[] {
    return this.suppliers.filter((supplier) =>
      supplier.lifecycleStatus === 'APPROVED' || supplier.lifecycleStatus === 'ACTIVE',
    );
  }

  ngOnInit(): void {
    this.loadRfqs();
    this.loadSuppliers();
  }

  protected loadRfqs(): void {
    this.loading = true;
    this.errorMessage = '';
    this.rfqService.getRfqs(this.page, this.pageSize, this.searchText, this.statusFilter).subscribe({
      next: (response) => {
        this.rfqs = response.data?.items ?? [];
        this.totalElements = response.data?.totalElements ?? 0;
        this.totalPages = response.data?.totalPages ?? 0;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 403
          ? 'Your account is not authorized to view sourcing events.'
          : error.error?.message ?? 'Unable to load RFQs.';
        this.loading = false;
      },
    });
  }

  protected applyRfqFilters(): void {
    this.page = 0;
    this.loadRfqs();
  }

  protected changeRfqPage(delta: number): void {
    const next = this.page + delta;
    if (next < 0 || next >= this.totalPages) return;
    this.page = next;
    this.loadRfqs();
  }

  protected loadSuppliers(): void {
    this.supplierLoading = true;
    this.supplierService.getSuppliers().subscribe({
      next: (response) => {
        this.suppliers = response.data ?? [];
        this.supplierLoading = false;
      },
      error: () => {
        this.supplierLoading = false;
      },
    });
  }

  protected openRfq(rfq: RfqSummary): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.rfqService.getRfq(rfq.rfqId).subscribe({
      next: (response) => {
        this.selectedRfq = response.data;
        this.selectedSupplierIds = [];
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to open this RFQ.';
      },
    });
  }

  protected addLine(): void {
    this.lines = [...this.lines, this.emptyLine()];
  }

  protected removeLine(index: number): void {
    if (this.lines.length > 1) this.lines = this.lines.filter((_, lineIndex) => lineIndex !== index);
  }

  protected createRfq(): void {
    if (this.creating) return;
    this.creating = true;
    this.errorMessage = '';
    this.successMessage = '';

    const payload: RfqCreatePayload = {
      ...this.form,
      submissionDeadline: new Date(this.form.submissionDeadline).toISOString(),
      requiredDeliveryDate: this.form.requiredDeliveryDate || null,
      lines: this.lines.map((line) => ({
        sapMaterialCode: line.sapMaterialCode.trim(),
        description: line.description.trim(),
        drawingRevision: line.drawingRevision.trim() || null,
        quantity: Number(line.quantity),
        uomCode: line.uomCode.trim().toUpperCase(),
        plantCode: line.plantCode.trim() || null,
        technicalSpecifications: line.technicalSpecifications.trim() || null,
      })),
    };

    this.rfqService.createRfq(payload).subscribe({
      next: (response) => {
        this.selectedRfq = response.data;
        this.rfqs = [this.toSummary(response.data), ...this.rfqs];
        this.successMessage = response.message || `${response.data.rfqNumber} created as a draft.`;
        this.showCreateForm = false;
        this.form = this.emptyForm();
        this.lines = [this.emptyLine()];
        this.creating = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to create the RFQ. Check its required fields.';
        this.creating = false;
      },
    });
  }

  protected inviteSuppliers(): void {
    if (!this.selectedRfq || this.inviting || this.selectedSupplierIds.length === 0) return;
    this.inviting = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.rfqService.inviteSuppliers(this.selectedRfq.rfqId, this.selectedSupplierIds).subscribe({
      next: (response) => {
        this.selectedRfq = { ...this.selectedRfq!, invitations: response.data ?? [] };
        this.updateSummary(this.selectedRfq);
        this.successMessage = response.message || 'Approved suppliers invited.';
        this.selectedSupplierIds = [];
        this.inviting = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to invite the selected suppliers.';
        this.inviting = false;
      },
    });
  }

  protected publishRfq(): void {
    if (!this.selectedRfq || this.publishing) return;
    this.publishing = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.rfqService.publishRfq(this.selectedRfq.rfqId).subscribe({
      next: (response) => {
        this.selectedRfq = response.data;
        this.updateSummary(response.data);
        this.successMessage = response.message || 'RFQ published to its invited suppliers.';
        this.publishing = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to publish this RFQ.';
        this.publishing = false;
      },
    });
  }

  protected closeDetails(): void {
    this.selectedRfq = null;
    this.selectedSupplierIds = [];
  }

  private updateSummary(rfq: Rfq): void {
    this.rfqs = this.rfqs.map((item) =>
      item.rfqId === rfq.rfqId ? this.toSummary(rfq) : item,
    );
  }

  private toSummary(rfq: Rfq): RfqSummary {
    return {
      rfqId: rfq.rfqId,
      rfqNumber: rfq.rfqNumber,
      title: rfq.title,
      status: rfq.status,
      currencyCode: rfq.currencyCode,
      submissionDeadline: rfq.submissionDeadline,
      lineCount: rfq.lines.length,
      invitationCount: rfq.invitations.length,
      createdAt: rfq.createdAt,
    };
  }

  private emptyForm() {
    return {
      title: '',
      description: '',
      projectCode: '',
      department: '',
      plantCode: '',
      purchasingGroup: '',
      currencyCode: 'INR',
      submissionDeadline: this.toLocalInputValue(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)),
      requiredDeliveryDate: '',
      paymentTerms: '',
      incoterms: '',
    };
  }

  private emptyLine(): RfqLineDraft {
    return {
      sapMaterialCode: '',
      description: '',
      drawingRevision: '',
      quantity: '1',
      uomCode: 'EA',
      plantCode: '',
      technicalSpecifications: '',
    };
  }

  private toLocalInputValue(date: Date): string {
    const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return localDate.toISOString().slice(0, 16);
  }
}
