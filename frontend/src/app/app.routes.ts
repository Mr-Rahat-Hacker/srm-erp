import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { SuppliersComponent } from './features/suppliers/suppliers.component';
import { RfqsComponent } from './features/rfqs/rfqs.component';
import { SupplierQuotationsComponent } from './features/quotations/supplier-quotations.component';
import { QuotationComparisonComponent } from './features/quotations/quotation-comparison.component';
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
  { path: 'quotations', component: QuotationComparisonComponent, canActivate: [authGuard] },
  { path: 'approvals', component: ModulePlaceholderComponent, canActivate: [authGuard], data: { title: 'Approvals', description: 'Review assigned supplier, sourcing and award approval tasks.', status: 'Planned module' } },
  { path: 'reports', component: ModulePlaceholderComponent, canActivate: [authGuard], data: { title: 'Reports & Audit', description: 'Explore SRM activity, workflow history and audit records.', status: 'Planned module' } },
  { path: '**', redirectTo: 'dashboard' },
];
