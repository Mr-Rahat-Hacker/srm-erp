import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SupplierService } from './supplier.service';
import {
  ApiResponse,
  SapVendorMapping,
  Supplier,
  SupplierLifecycleResult,
  SupplierRegistration,
} from './supplier.models';
import { SupplierDocumentsComponent } from './supplier-documents.component';

@Component({
  selector: 'app-suppliers',
  imports: [FormsModule, DatePipe, SupplierDocumentsComponent],
  templateUrl: './suppliers.component.html',
  styleUrl: './suppliers.component.scss',
})
export class SuppliersComponent implements OnInit {
  private readonly supplierService = inject(SupplierService);

  protected suppliers: Supplier[] = [];
  protected loading = false;
  protected saving = false;
  protected detailsLoading = false;
  protected lifecycleBusy = false;
  protected lifecycleRemarks = '';
  protected showForm = false;
  protected selectedSupplier: Supplier | null = null;
  protected successMessage = '';
  protected errorMessage = '';
  protected sapMappings: SapVendorMapping[] = [];
  protected mappingLoading = false;
  protected mappingSaving = false;
  protected mappingForm = { sapSystemCode: '', sapVendorCode: '', reason: '' };
  protected mappingDeactivateReason = '';

  protected form: SupplierRegistration = this.emptyForm();

  ngOnInit(): void {
    this.loadSuppliers();
  }

  protected loadSuppliers(): void {
    this.loading = true;
    this.errorMessage = '';
    this.supplierService.getSuppliers().subscribe({
      next: (response) => {
        this.suppliers = response.data ?? [];
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 401
          ? 'Your session has expired. Please sign in again.'
          : error.status === 403
            ? 'Your account does not have permission to view supplier records.'
            : 'Unable to load suppliers. Check that the backend is running and try again.';
        this.loading = false;
      },
    });
  }

  protected viewDetails(supplier: Supplier): void {
    this.selectedSupplier = null;
    this.sapMappings = [];
    this.mappingForm = { sapSystemCode: '', sapVendorCode: '', reason: '' };
    this.detailsLoading = true;
    this.supplierService.getSupplier(supplier.supplierId).subscribe({
      next: (response) => {
        this.selectedSupplier = response.data;
        this.lifecycleRemarks = '';
        this.detailsLoading = false;
        this.loadSapMappings(response.data.supplierId);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 403
          ? 'You do not have permission to view this supplier.'
          : error.status === 404
            ? 'This supplier record could not be found.'
            : 'Unable to load supplier details. Please try again.';
        this.detailsLoading = false;
      },
    });
  }

  protected closeDetails(): void {
    this.selectedSupplier = null;
    this.lifecycleRemarks = '';
    this.sapMappings = [];
    this.mappingForm = { sapSystemCode: '', sapVendorCode: '', reason: '' };
  }

  private loadSapMappings(supplierId: string): void {
    this.mappingLoading = true;
    this.supplierService.getSapVendorMappings(supplierId).subscribe({
      next: (response) => {
        this.sapMappings = response.data ?? [];
        this.mappingLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 403
          ? 'Your account cannot view SAP Vendor Code mappings.'
          : 'Unable to load SAP Vendor Code mappings.';
        this.mappingLoading = false;
      },
    });
  }

  protected saveSapMapping(): void {
    if (!this.selectedSupplier || this.mappingSaving) return;
    const payload = {
      sapSystemCode: this.mappingForm.sapSystemCode.trim(),
      sapVendorCode: this.mappingForm.sapVendorCode.trim(),
      reason: this.mappingForm.reason.trim(),
    };
    if (!payload.sapSystemCode || !payload.sapVendorCode || !payload.reason) {
      this.errorMessage = 'Enter the SAP system, vendor code, and reason.';
      return;
    }
    this.mappingSaving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.supplierService.saveSapVendorMapping(this.selectedSupplier.supplierId, payload).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'SAP Vendor Code mapping saved.';
        this.mappingForm = { sapSystemCode: '', sapVendorCode: '', reason: '' };
        this.mappingSaving = false;
        this.loadSapMappings(this.selectedSupplier!.supplierId);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to save SAP Vendor Code mapping.';
        this.mappingSaving = false;
      },
    });
  }

  protected deactivateSapMapping(mapping: SapVendorMapping): void {
    if (!this.selectedSupplier || this.mappingSaving) return;
    const reason = this.mappingDeactivateReason.trim();
    if (!reason) {
      this.errorMessage = 'Add a reason before deactivating a mapping.';
      return;
    }
    this.mappingSaving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.supplierService.deactivateSapVendorMapping(
      this.selectedSupplier.supplierId, mapping.sapSystemCode, reason,
    ).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'SAP Vendor Code mapping deactivated.';
        this.mappingDeactivateReason = '';
        this.mappingSaving = false;
        this.loadSapMappings(this.selectedSupplier!.supplierId);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to deactivate SAP Vendor Code mapping.';
        this.mappingSaving = false;
      },
    });
  }

  protected submitForReview(): void {
    if (!this.selectedSupplier) return;
    this.runLifecycleAction(this.supplierService.submitSupplier(this.selectedSupplier.supplierId));
  }

  protected startSupplierReview(): void {
    if (!this.selectedSupplier) return;
    this.runLifecycleAction(this.supplierService.startSupplierReview(this.selectedSupplier.supplierId));
  }

  protected decideSupplier(decision: 'APPROVE' | 'RETURN' | 'REJECT'): void {
    if (!this.selectedSupplier) return;
    const remarks = this.lifecycleRemarks.trim();
    if (decision !== 'APPROVE' && !remarks) {
      this.errorMessage = 'Add a reason before returning or rejecting this supplier.';
      return;
    }
    this.runLifecycleAction(
      this.supplierService.decideSupplier(this.selectedSupplier.supplierId, decision, remarks),
    );
  }

  private runLifecycleAction(request: Observable<ApiResponse<SupplierLifecycleResult>>): void {
    if (!this.selectedSupplier || this.lifecycleBusy) return;
    this.lifecycleBusy = true;
    this.errorMessage = '';
    this.successMessage = '';
    request.subscribe({
      next: (response) => {
        const supplierId = response.data.supplierId;
        const currentStatus = response.data.currentStatus;
        this.selectedSupplier = { ...this.selectedSupplier!, lifecycleStatus: currentStatus };
        this.suppliers = this.suppliers.map((supplier) =>
          supplier.supplierId === supplierId ? { ...supplier, lifecycleStatus: currentStatus } : supplier,
        );
        this.successMessage = response.message || `Supplier status changed to ${currentStatus}.`;
        this.lifecycleRemarks = '';
        this.lifecycleBusy = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 403
          ? 'Your account is not allowed to perform this supplier workflow action.'
          : error.status === 409
            ? error.error?.message ?? 'The supplier status changed. Refresh the profile and try again.'
            : error.error?.message ?? 'Unable to update the supplier status.';
        this.lifecycleBusy = false;
      },
    });
  }

  protected submit(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.saving = true;

    const payload = {
      ...this.form,
      tradeName: this.form.tradeName.trim(),
      gstin: this.form.gstin.trim().toUpperCase(),
      pan: this.form.pan.trim().toUpperCase(),
      countryCode: this.form.countryCode.trim().toUpperCase(),
    };

    this.supplierService.registerSupplier(payload).subscribe({
      next: (response) => {
        this.successMessage = `${response.data.supplierNumber} registered successfully.`;
        this.suppliers = [response.data, ...this.suppliers];
        this.form = this.emptyForm();
        this.showForm = false;
        this.saving = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Supplier registration failed. Please verify the details and try again.';
        this.saving = false;
      },
    });
  }

  protected resetForm(): void {
    this.form = this.emptyForm();
    this.errorMessage = '';
    this.successMessage = '';
  }

  private emptyForm(): SupplierRegistration {
    return {
      legalName: '', tradeName: '', gstin: '', pan: '',
      contactName: '', email: '', phone: '',
      addressLine1: '', city: '', stateRegion: '',
      postalCode: '', countryCode: 'IN',
    };
  }
}
