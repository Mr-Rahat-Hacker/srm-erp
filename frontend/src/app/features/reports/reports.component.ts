import { Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { ReportsService } from './reports.service';
import { ReportsDashboard } from './reports.models';

@Component({
  selector: 'app-reports',
  imports: [DecimalPipe],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.scss',
})
export class ReportsComponent implements OnInit {
  private readonly reports = inject(ReportsService);
  dashboard: ReportsDashboard | null = null;
  loading = true;
  error = '';

  ngOnInit(): void {
    this.reports.dashboard().subscribe({
      next: (response) => { this.dashboard = response.data; this.loading = false; },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.message ?? 'Unable to load role-scoped reports.';
        this.loading = false;
      },
    });
  }

  statuses(map: Record<string, number> | undefined): Array<{ label: string; value: number }> {
    return Object.entries(map ?? {}).map(([label, value]) => ({ label, value }));
  }
}
