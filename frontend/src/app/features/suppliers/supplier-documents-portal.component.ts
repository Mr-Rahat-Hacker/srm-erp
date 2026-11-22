import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { SupplierDocumentsComponent } from './supplier-documents.component';
import { Supplier, SupplierProfileUpdate } from './supplier.models';
import { SupplierService } from './supplier.service';

@Component({
  selector: 'app-supplier-documents-portal',
  imports: [FormsModule, SupplierDocumentsComponent],
  templateUrl: './supplier-documents-portal.component.html',
  styleUrl: './supplier-documents-portal.component.scss',
})
export class SupplierDocumentsPortalComponent implements OnInit {
  private readonly suppliers = inject(SupplierService);

  protected profiles: Supplier[] = [];
  protected selectedSupplierId = '';
  protected profileForm: SupplierProfileUpdate = this.emptyProfileForm();
  protected loading = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected submitting = false;
  protected savingProfile = false;

  protected get selectedProfile(): Supplier | undefined {
    return this.profiles.find((profile) => profile.supplierId === this.selectedSupplierId);
  }

  protected get profileEditable(): boolean {
    return this.selectedProfile?.lifecycleStatus === 'DRAFT'
      || this.selectedProfile?.lifecycleStatus === 'CORRECTION_REQUIRED';
  }

  protected selectProfile(supplierId: string): void {
    this.selectedSupplierId = supplierId;
    const profile = this.profiles.find((item) => item.supplierId === supplierId);
    this.profileForm = {
      legalName: profile?.legalName ?? '',
      tradeName: profile?.tradeName ?? '',
      gstin: profile?.gstin ?? '',
      pan: profile?.pan ?? '',
      contactName: profile?.contactName ?? '',
      email: profile?.email ?? '',
      phone: profile?.phone ?? '',
      addressLine1: '',
      city: '',
      stateRegion: '',
      postalCode: '',
      countryCode: '',
    };
    this.errorMessage = '';
    this.successMessage = '';
  }

  protected saveProfile(): void {
    const profile = this.selectedProfile;
    if (!profile || !this.profileEditable || this.savingProfile) return;
    this.savingProfile = true;
    this.errorMessage = '';
    this.successMessage = '';
    const payload = Object.fromEntries(
      Object.entries(this.profileForm).map(([key, value]) => [key, value?.trim() || undefined]),
    ) as SupplierProfileUpdate;
    if (payload.gstin) payload.gstin = payload.gstin.toUpperCase();
    if (payload.pan) payload.pan = payload.pan.toUpperCase();
    if (payload.countryCode) payload.countryCode = payload.countryCode.toUpperCase();
    this.suppliers.updateMySupplierProfile(profile.supplierId, payload).subscribe({
      next: (response) => {
        this.profiles = this.profiles.map((item) => item.supplierId === profile.supplierId ? response.data : item);
        this.selectProfile(profile.supplierId);
        this.successMessage = response.message;
        this.savingProfile = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to update this supplier profile.';
        this.savingProfile = false;
      },
    });
  }

  protected submitProfile(): void {
    const profile = this.selectedProfile;
    if (!profile || this.submitting) return;
    this.submitting = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.suppliers.submitMySupplierProfile(profile.supplierId).subscribe({
      next: (response) => {
        this.profiles = this.profiles.map((item) => item.supplierId === profile.supplierId
          ? { ...item, lifecycleStatus: response.data.currentStatus } : item);
        this.successMessage = response.message;
        this.submitting = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to submit this supplier profile.';
        this.submitting = false;
      },
    });
  }

  ngOnInit(): void {
    this.loading = true;
    this.suppliers.getMySupplierProfiles().subscribe({
      next: (response) => {
        this.profiles = response.data ?? [];
        this.loading = false;
        this.selectProfile(this.profiles[0]?.supplierId ?? '');
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load your supplier profile.';
        this.loading = false;
      },
    });
  }

  private emptyProfileForm(): SupplierProfileUpdate {
    return {
      legalName: '', tradeName: '', gstin: '', pan: '',
      contactName: '', email: '', phone: '',
      addressLine1: '', city: '', stateRegion: '', postalCode: '', countryCode: '',
    };
  }
}
