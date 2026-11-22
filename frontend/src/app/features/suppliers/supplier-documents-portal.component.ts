import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { SupplierDocumentsComponent } from './supplier-documents.component';
import { Supplier } from './supplier.models';
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
  protected loading = false;
  protected errorMessage = '';

  ngOnInit(): void {
    this.loading = true;
    this.suppliers.getMySupplierProfiles().subscribe({
      next: (response) => {
        this.profiles = response.data ?? [];
        this.selectedSupplierId = this.profiles[0]?.supplierId ?? '';
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Unable to load your supplier profile.';
        this.loading = false;
      },
    });
  }
}
