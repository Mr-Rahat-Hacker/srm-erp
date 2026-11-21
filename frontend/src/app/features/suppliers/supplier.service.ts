import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Supplier, SupplierRegistration } from './supplier.models';

const API_BASE_URL = '/api/v1';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);

  getSuppliers(): Observable<ApiResponse<Supplier[]>> {
    return this.http.get<ApiResponse<Supplier[]>>(`${API_BASE_URL}/suppliers`);
  }

  getSupplier(supplierId: string): Observable<ApiResponse<Supplier>> {
    return this.http.get<ApiResponse<Supplier>>(`${API_BASE_URL}/suppliers/${supplierId}`);
  }

  registerSupplier(payload: SupplierRegistration): Observable<ApiResponse<Supplier>> {
    return this.http.post<ApiResponse<Supplier>>(`${API_BASE_URL}/suppliers`, payload);
  }
}
