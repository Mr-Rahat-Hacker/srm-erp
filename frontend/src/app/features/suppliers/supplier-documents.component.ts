import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, Input, OnChanges, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SupplierDocument } from './supplier.models';
import { SupplierService } from './supplier.service';

@Component({
  selector: 'app-supplier-documents',
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './supplier-documents.component.html',
  styleUrl: './supplier-documents.component.scss',
})
export class SupplierDocumentsComponent implements OnChanges {
  @Input({ required: true }) supplierId = '';

  private readonly supplierService = inject(SupplierService);

  protected readonly documentTypes = [
    { value: 'GST_CERTIFICATE', label: 'GST certificate' },
    { value: 'PAN_CARD', label: 'PAN card' },
    { value: 'ISO_CERTIFICATE', label: 'ISO certificate' },
    { value: 'BANK_PROOF', label: 'Bank proof' },
    { value: 'MSME_CERTIFICATE', label: 'MSME certificate' },
    { value: 'OTHER', label: 'Other' },
  ];

  protected documents: SupplierDocument[] = [];
  protected documentType = 'GST_CERTIFICATE';
  protected expiryDate = '';
  protected selectedFile: File | null = null;
  protected reviewRemarks = '';
  protected loading = false;
  protected uploading = false;
  protected downloadingId = '';
  protected reviewingId = '';
  protected errorMessage = '';
  protected successMessage = '';

  protected get hasPendingReviews(): boolean {
    return this.documents.some((item) => item.reviewStatus === 'PENDING');
  }

  ngOnChanges(): void {
    if (this.supplierId) this.loadDocuments();
  }

  protected selectFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  protected loadDocuments(): void {
    this.loading = true;
    this.errorMessage = '';
    this.supplierService.getDocuments(this.supplierId).subscribe({
      next: (response) => {
        this.documents = response.data ?? [];
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 403
          ? 'Your account does not have permission to view supplier documents.'
          : error.status === 401
            ? 'Your session has expired. Please sign in again.'
            : error.error?.message ?? 'Unable to load supplier documents.';
        this.loading = false;
      },
    });
  }

  protected upload(): void {
    if (!this.selectedFile || this.uploading) return;
    this.uploading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.supplierService.uploadDocument(
      this.supplierId,
      this.documentType,
      this.selectedFile,
      this.expiryDate,
    ).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'Document uploaded for verification.';
        this.selectedFile = null;
        this.expiryDate = '';
        this.uploading = false;
        this.loadDocuments();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Document upload failed. Check permissions and file type.';
        this.uploading = false;
      },
    });
  }

  protected download(fileRecord: SupplierDocument): void {
    this.downloadingId = fileRecord.documentId;
    this.errorMessage = '';
    this.supplierService.downloadDocument(this.supplierId, fileRecord.documentId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = globalThis.document.createElement('a');
        link.href = url;
        link.download = fileRecord.originalFilename;
        link.click();
        URL.revokeObjectURL(url);
        this.downloadingId = '';
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to download this document.';
        this.downloadingId = '';
      },
    });
  }

  protected review(fileRecord: SupplierDocument, decision: 'VERIFY' | 'REJECT'): void {
    this.reviewingId = fileRecord.documentId;
    this.errorMessage = '';
    this.successMessage = '';
    this.supplierService.reviewDocument(
      this.supplierId,
      fileRecord.documentId,
      decision,
      this.reviewRemarks.trim(),
    ).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'Document review saved.';
        this.reviewRemarks = '';
        this.reviewingId = '';
        this.loadDocuments();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to save the document review.';
        this.reviewingId = '';
      },
    });
  }

  protected labelFor(type: string): string {
    return this.documentTypes.find((item) => item.value === type)?.label ?? type;
  }
}
