import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { ReportsService } from '../reports/reports.service';
import { ReportsDashboard } from '../reports/reports.models';

@Component({
  selector: 'app-dashboard',
  imports: [DecimalPipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly reports = inject(ReportsService);

  protected dashboard: ReportsDashboard | null = null;
  protected loading = true;
  protected error = '';

  ngOnInit(): void {
    this.reports.dashboard().subscribe({
      next: (response) => { this.dashboard = response.data; this.loading = false; },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.message ?? 'Unable to load your role-scoped dashboard.';
        this.loading = false;
      },
    });
  }

  protected get isSupplier(): boolean {
    const roles = this.auth.getUser()?.roles ?? [];
    return roles.includes('SUPPLIER_USER') && !roles.some((role) => role !== 'SUPPLIER_USER');
  }

  protected get actions(): Array<{ label: string; path: string }> {
    if (this.isSupplier) {
      return [
        { label: 'Invited RFQs and quotations', path: '/supplier-portal/quotations' },
        { label: 'Company documents', path: '/supplier-portal/documents' },
        { label: 'Negotiation requests', path: '/supplier-portal/negotiations' },
      ];
    }

    const roles = this.auth.getUser()?.roles ?? [];
    const actions: Array<{ label: string; path: string }> = [];
    if (roles.some((role) => ['SUPPLIER_VERIFIER', 'SRM_ADMIN', 'SUPER_ADMIN', 'PROCUREMENT_BUYER', 'AUDITOR'].includes(role))) {
      actions.push({ label: 'Supplier qualification', path: '/suppliers' });
    }
    if (roles.includes('PROCUREMENT_BUYER')) {
      actions.push(
        { label: 'RFQ management', path: '/rfqs' },
        { label: 'Quotation comparison', path: '/quotations' },
        { label: 'Evaluations', path: '/evaluations' },
        { label: 'Negotiations', path: '/negotiations' },
      );
    }
    if (roles.includes('PROCUREMENT_APPROVER')) actions.push({ label: 'Assigned approvals', path: '/approvals' });
    if (roles.includes('SUPER_ADMIN')) actions.push({ label: 'User administration', path: '/admin/users' });
    if (roles.some((role) => ['AUDITOR', 'SRM_ADMIN', 'SUPER_ADMIN', 'PROCUREMENT_BUYER', 'PROCUREMENT_APPROVER'].includes(role))) {
      actions.push({ label: 'Reports and audit trail', path: '/reports' });
    }
    return actions;
  }

  protected statuses(map: Record<string, number> | undefined): Array<{ label: string; value: number }> {
    return Object.entries(map ?? {}).map(([label, value]) => ({ label, value }));
  }
}
