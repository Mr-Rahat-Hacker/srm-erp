import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected get navigation() {
    const roles = this.currentUser?.roles ?? [];
    const items = [{ label: 'Overview', path: '/dashboard', icon: '⌂' }];
    const supplierUser = roles.includes('SUPPLIER_USER');
    if (supplierUser) {
      items.push({ label: 'Invited RFQs & quotations', path: '/supplier-portal/quotations', icon: '▧' });
      items.push({ label: 'Company documents', path: '/supplier-portal/documents', icon: '▤' });
      items.push({ label: 'Negotiation requests', path: '/supplier-portal/negotiations', icon: '↔' });
    }

    const supplierRead = roles.some((role) => [
      'SUPPLIER_VERIFIER', 'PROCUREMENT_BUYER', 'SRM_ADMIN', 'SUPER_ADMIN', 'AUDITOR',
    ].includes(role));
    if (supplierRead) items.push({ label: 'Suppliers', path: '/suppliers', icon: '▤' });

    const rfqRead = roles.some((role) => [
      'PROCUREMENT_BUYER', 'SUPPLIER_VERIFIER', 'SRM_ADMIN', 'SUPER_ADMIN', 'AUDITOR',
    ].includes(role));
    if (rfqRead) items.push({ label: 'RFQ Management', path: '/rfqs', icon: '◎' });

    if (roles.includes('PROCUREMENT_BUYER')) {
      items.push({ label: 'Quotations', path: '/quotations', icon: '▧' });
      items.push({ label: 'Evaluations', path: '/evaluations', icon: '◇' });
      items.push({ label: 'Negotiations', path: '/negotiations', icon: '↔' });
    }
    if (roles.includes('PROCUREMENT_APPROVER')) {
      items.push({ label: 'Approvals', path: '/approvals', icon: '✓' });
    }

    if (roles.length > 0) items.push({ label: 'Notifications', path: '/notifications', icon: '◉' });
    if (roles.some((role) => [
      'AUDITOR', 'SRM_ADMIN', 'SUPER_ADMIN', 'PROCUREMENT_BUYER', 'PROCUREMENT_APPROVER',
    ].includes(role))) {
      items.push({ label: 'Audit trail', path: '/audit', icon: '≋' });
    }
    if (roles.some((role) => ['SRM_ADMIN', 'SUPER_ADMIN'].includes(role))) {
      items.push({ label: 'Approval policies', path: '/admin/approval-policies', icon: '⇢' });
    }
    if (roles.includes('SUPER_ADMIN')) {
      items.push({ label: 'User administration', path: '/admin/users', icon: '♙' });
    }
    if (roles.length > 0) items.push({ label: 'Reports', path: '/reports', icon: '▥' });
    return items;
  }

  protected get currentUser() {
    return this.auth.getUser();
  }

  protected get userInitial(): string {
    return this.currentUser?.fullName?.trim().charAt(0).toUpperCase() || 'U';
  }

  protected logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
