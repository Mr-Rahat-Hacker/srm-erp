import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { QuotationComparison, RfqSummary } from './comparison.models';
import { QuotationComparisonService } from './quotation-comparison.service';

@Component({
  selector: 'app-quotation-comparison',
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './quotation-comparison.component.html',
  styleUrl: './quotation-comparison.component.scss',
})
export class QuotationComparisonComponent implements OnInit {
  private readonly service = inject(QuotationComparisonService);

  protected rfqs: RfqSummary[] = [];
  protected selectedRfqId = '';
  protected comparison: QuotationComparison | null = null;
  protected loadingRfqs = false;
  protected loadingComparison = false;
  protected errorMessage = '';

  ngOnInit(): void {
    this.loadingRfqs = true;
    this.service.getRfqs().subscribe({
      next: (response) => {
        this.rfqs = response.data ?? [];
        this.loadingRfqs = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 403
          ? 'Only the RFQ owner can compare its quotations.'
          : error.error?.message ?? 'Unable to load RFQs.';
        this.loadingRfqs = false;
      },
    });
  }

  protected loadComparison(): void {
    if (!this.selectedRfqId || this.loadingComparison) return;
    this.errorMessage = '';
    this.loadingComparison = true;
    this.service.compare(this.selectedRfqId).subscribe({
      next: (response) => {
        this.comparison = response.data;
        this.loadingComparison = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 403
          ? 'Your account cannot compare supplier quotations.'
          : error.error?.message ?? 'Unable to load quotation comparison.';
        this.loadingComparison = false;
      },
    });
  }

  protected quoteFor(supplierIndex: number, lineId: string) {
    return this.comparison?.suppliers[supplierIndex].lines.find((line) => line.rfqLineId === lineId);
  }
}
