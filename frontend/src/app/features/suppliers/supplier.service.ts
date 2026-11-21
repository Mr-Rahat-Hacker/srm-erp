import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Supplier, SupplierRegistration } from './supplier.models';

/**
 * Use a relative API path. During Codespaces development, Angular's dev-server
 * proxy forwards /api to Spring Boot on localhost:8080. The browser therefore
 * talks only to the frontend origin and does not make a cross-origin request.
 */
const API_BASE_URL = '/api/v1';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);

  getSuppliers(): Observable<ApiResponse<Supplier[]>> {
    return this.http.get<ApiResponse<Supplier[]>>(`${API_BASE_URL}/suppliers`);
  }

  registerSupplier(payload: SupplierRegistration): Observable<ApiResponse<Supplier>> {
    return this.http.post<ApiResponse<Supplier>>(`${API_BASE_URL}/suppliers`, payload);
  }
}
