import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiResponse,
  SapVendorMapping,
  SapVendorMappingRequest,
  Supplier,
  SupplierDocument,
  SupplierLifecycleResult,
  SupplierRegistration,
  SupplierAccountRegistration,
  SupplierProfileUpdate,
} from './supplier.models';

const API_BASE_URL = '/api/v1';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);

  getSuppliers(): Observable<ApiResponse<Supplier[]>> {
    return this.http.get<ApiResponse<Supplier[]>>(`${API_BASE_URL}/suppliers`);
  }

  getMySupplierProfiles(): Observable<ApiResponse<Supplier[]>> {
    return this.http.get<ApiResponse<Supplier[]>>(`${API_BASE_URL}/supplier/profiles`);
  }

  getSupplier(supplierId: string): Observable<ApiResponse<Supplier>> {
    return this.http.get<ApiResponse<Supplier>>(`${API_BASE_URL}/suppliers/${supplierId}`);
  }

  getSapVendorMappings(supplierId: string): Observable<ApiResponse<SapVendorMapping[]>> {
    return this.http.get<ApiResponse<SapVendorMapping[]>>(
      `${API_BASE_URL}/suppliers/${supplierId}/sap-vendor-mappings`,
    );
  }

  saveSapVendorMapping(
    supplierId: string,
    payload: SapVendorMappingRequest,
  ): Observable<ApiResponse<SapVendorMapping>> {
    return this.http.put<ApiResponse<SapVendorMapping>>(
      `${API_BASE_URL}/suppliers/${supplierId}/sap-vendor-mappings`,
      payload,
    );
  }

  deactivateSapVendorMapping(
    supplierId: string,
    systemCode: string,
    reason: string,
  ): Observable<ApiResponse<void>> {
    const encodedSystemCode = encodeURIComponent(systemCode);
    const encodedReason = encodeURIComponent(reason);
    return this.http.delete<ApiResponse<void>>(
      `${API_BASE_URL}/suppliers/${supplierId}/sap-vendor-mappings/${encodedSystemCode}?reason=${encodedReason}`,
    );
  }

  registerSupplier(payload: SupplierRegistration): Observable<ApiResponse<Supplier>> {
    return this.http.post<ApiResponse<Supplier>>(`${API_BASE_URL}/suppliers`, payload);
  }

  registerSupplierAccount(payload: SupplierAccountRegistration): Observable<ApiResponse<Supplier>> {
    return this.http.post<ApiResponse<Supplier>>(`${API_BASE_URL}/supplier/register`, payload);
  }

  updateMySupplierProfile(supplierId: string, payload: SupplierProfileUpdate): Observable<ApiResponse<Supplier>> {
    return this.http.patch<ApiResponse<Supplier>>(`${API_BASE_URL}/supplier/profiles/${supplierId}`, payload);
  }

  submitMySupplierProfile(supplierId: string): Observable<ApiResponse<SupplierLifecycleResult>> {
    return this.http.post<ApiResponse<SupplierLifecycleResult>>(
      `${API_BASE_URL}/supplier/profiles/${supplierId}/submit`, {},
    );
  }

  submitSupplier(supplierId: string): Observable<ApiResponse<SupplierLifecycleResult>> {
    return this.http.post<ApiResponse<SupplierLifecycleResult>>(
      `${API_BASE_URL}/suppliers/${supplierId}/submit`,
      {},
    );
  }

  startSupplierReview(supplierId: string): Observable<ApiResponse<SupplierLifecycleResult>> {
    return this.http.post<ApiResponse<SupplierLifecycleResult>>(
      `${API_BASE_URL}/suppliers/${supplierId}/start-review`,
      {},
    );
  }

  decideSupplier(
    supplierId: string,
    decision: 'APPROVE' | 'RETURN' | 'REJECT',
    remarks: string,
  ): Observable<ApiResponse<SupplierLifecycleResult>> {
    return this.http.post<ApiResponse<SupplierLifecycleResult>>(
      `${API_BASE_URL}/suppliers/${supplierId}/decision`,
      { decision, remarks },
    );
  }

  getDocuments(supplierId: string): Observable<ApiResponse<SupplierDocument[]>> {
    return this.http.get<ApiResponse<SupplierDocument[]>>(
      `${API_BASE_URL}/suppliers/${supplierId}/documents`,
    );
  }

  uploadDocument(
    supplierId: string,
    documentType: string,
    file: File,
    expiryDate: string,
  ): Observable<ApiResponse<SupplierDocument>> {
    const body = new FormData();
    body.append('documentType', documentType);
    body.append('file', file, file.name);
    if (expiryDate) body.append('expiryDate', expiryDate);
    return this.http.post<ApiResponse<SupplierDocument>>(
      `${API_BASE_URL}/suppliers/${supplierId}/documents`,
      body,
    );
  }

  downloadDocument(supplierId: string, documentId: string): Observable<Blob> {
    return this.http.get(
      `${API_BASE_URL}/suppliers/${supplierId}/documents/${documentId}/content`,
      { responseType: 'blob' },
    );
  }

  reviewDocument(
    supplierId: string,
    documentId: string,
    decision: 'VERIFY' | 'REJECT',
    remarks: string,
  ): Observable<ApiResponse<SupplierDocument>> {
    return this.http.post<ApiResponse<SupplierDocument>>(
      `${API_BASE_URL}/suppliers/${supplierId}/documents/${documentId}/review`,
      { decision, remarks },
    );
  }
}
