import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { SupplierService } from './supplier.service';
import { Supplier, SupplierRegistration } from './supplier.models';

@Component({
  selector: 'app-suppliers',
  imports: [FormsModule],
  templateUrl: './suppliers.component.html',
  styleUrl: './suppliers.component.scss',
})
export class SuppliersComponent implements OnInit {
  private readonly supplierService = inject(SupplierService);

  protected suppliers: Supplier[] = [];
  protected loading = false;
  protected saving = false;
  protected showForm = false;
  protected successMessage = '';
  protected errorMessage = '';

  protected form: SupplierRegistration = this.emptyForm();

  ngOnInit(): void {
    this.loadSuppliers();
  }

  protected loadSuppliers(): void {
    this.loading = true;
    this.supplierService.getSuppliers().subscribe({
      next: (response) => {
        this.suppliers = response.data ?? [];
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load suppliers. Check that the Java API is running.';
        this.loading = false;
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
