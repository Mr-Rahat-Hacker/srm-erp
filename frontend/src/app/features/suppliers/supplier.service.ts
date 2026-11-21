import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Supplier, SupplierRegistration } from './supplier.models';

/**
 * Codespaces maps each forwarded port to a different HTTPS origin. The 8080
 * forwarded URL can redirect unauthenticated/inactive port requests (302),
 * which browsers then report as a CORS failure. Use the Codespaces local
 * forwarded-port proxy route from the 4200 origin instead.
 *
 * In a Codespaces browser, /proxy/8080 is same-origin with Angular and avoids
 * cross-origin redirect/CORS handling. Local development falls back to 8080.
 */
const API_BASE_URL = window.location.hostname.endsWith('.app.github.dev')
  ? '/proxy/8080/api/v1'
  : 'http://localhost:8080/api/v1';

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
