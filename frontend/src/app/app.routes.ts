import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { SuppliersComponent } from './features/suppliers/suppliers.component';
import { RfqsComponent } from './features/rfqs/rfqs.component';
import { SupplierQuotationsComponent } from './features/quotations/supplier-quotations.component';
import { QuotationComparisonComponent } from './features/quotations/quotation-comparison.component';
import { BuyerNegotiationsComponent } from './features/negotiations/buyer-negotiations.component';
import { SupplierNegotiationsComponent } from './features/negotiations/supplier-negotiations.component';
import { EvaluationsComponent } from './features/evaluations/evaluations.component';
import { ApprovalsComponent } from './features/approvals/approvals.component';
import { AuditComponent } from './features/audit/audit.component';
import { NotificationsComponent } from './features/notifications/notifications.component';
import { ModulePlaceholderComponent } from './shared/components/module-placeholder/module-placeholder.component';
import { LoginComponent } from './features/auth/login.component';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'suppliers', component: SuppliersComponent, canActivate: [authGuard] },
  { path: 'rfqs', component: RfqsComponent, canActivate: [authGuard] },
  { path: 'supplier-portal/quotations', component: SupplierQuotationsComponent, canActivate: [authGuard] },
  { path: 'supplier-portal/negotiations', component: SupplierNegotiationsComponent, canActivate: [authGuard] },
  { path: 'quotations', component: QuotationComparisonComponent, canActivate: [authGuard] },
  { path: 'negotiations', component: BuyerNegotiationsComponent, canActivate: [authGuard] },
  { path: 'evaluations', component: EvaluationsComponent, canActivate: [authGuard] },
  { path: 'approvals', component: ApprovalsComponent, canActivate: [authGuard] },
  { path: 'audit', component: AuditComponent, canActivate: [authGuard] },
  { path: 'notifications', component: NotificationsComponent, canActivate: [authGuard] },
  { path: 'reports', component: ModulePlaceholderComponent, canActivate: [authGuard], data: { title: 'Reports & Audit', description: 'Explore SRM activity, workflow history and audit records.', status: 'Planned module' } },
  { path: '**', redirectTo: 'dashboard' },
];
