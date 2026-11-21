import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { SuppliersComponent } from './features/suppliers/suppliers.component';
import { RfqsComponent } from './features/rfqs/rfqs.component';
import { ModulePlaceholderComponent } from './shared/components/module-placeholder/module-placeholder.component';
import { LoginComponent } from './features/auth/login.component';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'suppliers', component: SuppliersComponent, canActivate: [authGuard] },
  { path: 'rfqs', component: RfqsComponent, canActivate: [authGuard] },
  { path: 'quotations', component: ModulePlaceholderComponent, canActivate: [authGuard], data: { title: 'Quotation Management', description: 'Receive supplier quotations, maintain revisions and compare commercial offers.', status: 'Planned module' } },
  { path: 'approvals', component: ModulePlaceholderComponent, canActivate: [authGuard], data: { title: 'Approvals', description: 'Review assigned supplier, sourcing and award approval tasks.', status: 'Planned module' } },
  { path: 'reports', component: ModulePlaceholderComponent, canActivate: [authGuard], data: { title: 'Reports & Audit', description: 'Explore SRM activity, workflow history and audit records.', status: 'Planned module' } },
  { path: '**', redirectTo: 'dashboard' },
];
