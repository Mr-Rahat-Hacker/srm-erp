import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { SuppliersComponent } from './features/suppliers/suppliers.component';
import { SupplierDocumentsPortalComponent } from './features/suppliers/supplier-documents-portal.component';
import { RfqsComponent } from './features/rfqs/rfqs.component';
import { SupplierQuotationsComponent } from './features/quotations/supplier-quotations.component';
import { QuotationComparisonComponent } from './features/quotations/quotation-comparison.component';
import { BuyerNegotiationsComponent } from './features/negotiations/buyer-negotiations.component';
import { SupplierNegotiationsComponent } from './features/negotiations/supplier-negotiations.component';
import { EvaluationsComponent } from './features/evaluations/evaluations.component';
import { ApprovalsComponent } from './features/approvals/approvals.component';
import { AuditComponent } from './features/audit/audit.component';
import { NotificationsComponent } from './features/notifications/notifications.component';
import { ReportsComponent } from './features/reports/reports.component';
import { LoginComponent } from './features/auth/login.component';
import { SupplierRegistrationComponent } from './features/auth/supplier-registration.component';
import { AdminUsersComponent } from './features/admin-users/admin-users.component';
import { ApprovalPoliciesComponent } from './features/approvals/approval-policies.component';
import { authGuard } from './core/auth/auth.guard';
import { superAdminGuard } from './core/auth/super-admin.guard';
import { approvalPolicyGuard } from './core/auth/approval-policy.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: SupplierRegistrationComponent },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'suppliers', component: SuppliersComponent, canActivate: [authGuard, roleGuard], data: { roles: ['SUPPLIER_VERIFIER', 'PROCUREMENT_BUYER', 'SRM_ADMIN', 'SUPER_ADMIN', 'AUDITOR'] } },
  { path: 'rfqs', component: RfqsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['PROCUREMENT_BUYER', 'SUPPLIER_VERIFIER', 'SRM_ADMIN', 'SUPER_ADMIN', 'AUDITOR'] } },
  { path: 'supplier-portal/quotations', component: SupplierQuotationsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['SUPPLIER_USER'] } },
  { path: 'supplier-portal/negotiations', component: SupplierNegotiationsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['SUPPLIER_USER'] } },
  { path: 'supplier-portal/documents', component: SupplierDocumentsPortalComponent, canActivate: [authGuard, roleGuard], data: { roles: ['SUPPLIER_USER'] } },
  { path: 'quotations', component: QuotationComparisonComponent, canActivate: [authGuard, roleGuard], data: { roles: ['PROCUREMENT_BUYER'] } },
  { path: 'negotiations', component: BuyerNegotiationsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['PROCUREMENT_BUYER'] } },
  { path: 'evaluations', component: EvaluationsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['PROCUREMENT_BUYER'] } },
  { path: 'approvals', component: ApprovalsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['PROCUREMENT_APPROVER'] } },
  { path: 'audit', component: AuditComponent, canActivate: [authGuard, roleGuard], data: { roles: ['AUDITOR', 'SRM_ADMIN', 'SUPER_ADMIN', 'PROCUREMENT_BUYER', 'PROCUREMENT_APPROVER'] } },
  { path: 'notifications', component: NotificationsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['SUPPLIER_USER', 'SUPPLIER_VERIFIER', 'PROCUREMENT_BUYER', 'PROCUREMENT_APPROVER', 'SRM_ADMIN', 'SUPER_ADMIN', 'AUDITOR'] } },
  { path: 'reports', component: ReportsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['SUPPLIER_USER', 'SUPPLIER_VERIFIER', 'PROCUREMENT_BUYER', 'PROCUREMENT_APPROVER', 'SRM_ADMIN', 'SUPER_ADMIN', 'AUDITOR'] } },
  { path: 'admin/approval-policies', component: ApprovalPoliciesComponent, canActivate: [authGuard, approvalPolicyGuard] },
  { path: 'admin/users', component: AdminUsersComponent, canActivate: [authGuard, superAdminGuard] },
  { path: '**', redirectTo: 'dashboard' },
];
