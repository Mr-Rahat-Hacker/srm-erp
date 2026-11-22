import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { SupplierService } from './supplier.service';

describe('SupplierService paged list', () => {
  let service: SupplierService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SupplierService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SupplierService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends page, size, search, and status filters to the paged endpoint', () => {
    let received: unknown;
    service.getSuppliersPage(2, 10, 'acme parts', 'APPROVED').subscribe((response) => {
      received = response.data;
    });

    const request = http.expectOne('/api/v1/suppliers/page?page=2&size=10&q=acme+parts&status=APPROVED');
    expect(request.request.method).toBe('GET');
    request.flush({
      success: true,
      message: 'Suppliers retrieved successfully',
      data: { items: [], page: 2, size: 10, totalElements: 21, totalPages: 3 },
      timestamp: new Date().toISOString(),
    });

    expect(received).toEqual({ items: [], page: 2, size: 10, totalElements: 21, totalPages: 3 });
  });
});
