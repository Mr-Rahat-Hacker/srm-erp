import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Supplier, SupplierRegistration } from './supplier.models';

const apiHost = window.location.hostname.replace('-4200.', '-8080.');
const API_URL = `${window.location.protocol}//${apiHost}/api/v1/suppliers`;

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);

  getSuppliers(): Observable<ApiResponse<Supplier[]>> {
    return this.http.get<ApiResponse<Supplier[]>>(API_URL);
  }

  registerSupplier(payload: SupplierRegistration): Observable<ApiResponse<Supplier>> {
    return this.http.post<ApiResponse<Supplier>>(API_URL, payload);
  }
}
