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
    if (roles.includes('SUPPLIER_USER') && !roles.some((role) => role !== 'SUPPLIER_USER')) {
      return [
        { label: 'Overview', path: '/dashboard', icon: '⌂' },
        { label: 'Invited RFQs & quotations', path: '/supplier-portal/quotations', icon: '▧' },
        { label: 'Negotiation requests', path: '/supplier-portal/negotiations', icon: '↔' },
        { label: 'Notifications', path: '/notifications', icon: '◉' },
      ];
    }
    const items = [
      { label: 'Overview', path: '/dashboard', icon: '⌂' },
      { label: 'Suppliers', path: '/suppliers', icon: '▤' },
      { label: 'RFQ Management', path: '/rfqs', icon: '◎' },
      { label: 'Quotations', path: '/quotations', icon: '▧' },
      { label: 'Evaluations', path: '/evaluations', icon: '◇' },
      { label: 'Negotiations', path: '/negotiations', icon: '↔' },
    ];
    items.push({ label: 'Notifications', path: '/notifications', icon: '◉' });
    if (roles.includes('PROCUREMENT_APPROVER')) {
      items.push({ label: 'Approvals', path: '/approvals', icon: '✓' });
    }
    if (roles.some((role) => ['AUDITOR', 'SRM_ADMIN', 'SUPER_ADMIN', 'PROCUREMENT_BUYER', 'PROCUREMENT_APPROVER'].includes(role))) {
      items.push({ label: 'Audit trail', path: '/audit', icon: '≋' });
    }
    items.push({ label: 'Reports', path: '/reports', icon: '▥' });
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
