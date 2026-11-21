import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ModulePlaceholderComponent } from './shared/components/module-placeholder/module-placeholder.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'suppliers', component: ModulePlaceholderComponent, data: { title: 'Supplier Management', description: 'Supplier registration, company profiles, documents and qualification workflow.', status: 'Next module to implement' } },
  { path: 'rfqs', component: ModulePlaceholderComponent, data: { title: 'RFQ Management', description: 'Create sourcing events, maintain RFQ line references and invite qualified suppliers.', status: 'Planned module' } },
  { path: 'quotations', component: ModulePlaceholderComponent, data: { title: 'Quotation Management', description: 'Receive supplier quotations, maintain revisions and compare commercial offers.', status: 'Planned module' } },
  { path: 'approvals', component: ModulePlaceholderComponent, data: { title: 'Approvals', description: 'Review assigned supplier, sourcing and award approval tasks.', status: 'Planned module' } },
  { path: 'reports', component: ModulePlaceholderComponent, data: { title: 'Reports & Audit', description: 'Explore SRM activity, workflow history and audit records.', status: 'Planned module' } },
  { path: '**', redirectTo: 'dashboard' },
];
