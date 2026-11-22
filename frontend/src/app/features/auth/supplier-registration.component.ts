import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SupplierAccountRegistration } from '../suppliers/supplier.models';
import { SupplierService } from '../suppliers/supplier.service';

@Component({
  selector: 'app-supplier-registration',
  imports: [FormsModule, RouterLink],
  templateUrl: './supplier-registration.component.html',
  styleUrl: './supplier-registration.component.scss',
})
export class SupplierRegistrationComponent {
  private readonly suppliers = inject(SupplierService);

  protected form: SupplierAccountRegistration = this.emptyForm();
  protected passwordConfirmation = '';
  protected loading = false;
  protected errorMessage = '';
  protected successMessage = '';

  protected submit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (this.form.password !== this.passwordConfirmation) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }
    this.loading = true;
    const payload = {
      ...this.form,
      email: this.form.email.trim(),
      legalName: this.form.legalName.trim(),
      contactName: this.form.contactName.trim(),
      tradeName: this.form.tradeName.trim(),
      gstin: this.form.gstin.trim().toUpperCase(),
      pan: this.form.pan.trim().toUpperCase(),
      countryCode: this.form.countryCode.trim().toUpperCase(),
    };
    this.suppliers.registerSupplierAccount(payload).subscribe({
      next: (response) => {
        this.successMessage = response.message;
        this.form = this.emptyForm();
        this.passwordConfirmation = '';
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Registration failed. Check the information and try again.';
        this.loading = false;
      },
    });
  }

  private emptyForm(): SupplierAccountRegistration {
    return {
      legalName: '', tradeName: '', gstin: '', pan: '',
      contactName: '', email: '', phone: '',
      addressLine1: '', city: '', stateRegion: '',
      postalCode: '', countryCode: 'IN', password: '',
    };
  }
}
