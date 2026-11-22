import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { RfqService } from './rfq.service';

describe('RfqService paged list', () => {
  let service: RfqService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [RfqService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RfqService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends page, size, search, and status filters to the paged endpoint', () => {
    let received: unknown;
    service.getRfqs(1, 5, 'RFQ-12', 'PUBLISHED').subscribe((response) => {
      received = response.data;
    });

    const request = http.expectOne('/api/v1/rfqs/page?page=1&size=5&q=RFQ-12&status=PUBLISHED');
    expect(request.request.method).toBe('GET');
    request.flush({
      success: true,
      message: 'RFQs retrieved successfully',
      data: { items: [], page: 1, size: 5, totalElements: 6, totalPages: 2 },
      timestamp: new Date().toISOString(),
    });

    expect(received).toEqual({ items: [], page: 1, size: 5, totalElements: 6, totalPages: 2 });
  });
});
