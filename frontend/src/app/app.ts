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

  protected readonly navigation = [
    { label: 'Overview', path: '/dashboard', icon: '⌂' },
    { label: 'Suppliers', path: '/suppliers', icon: '▤' },
    { label: 'RFQ Management', path: '/rfqs', icon: '◎' },
    { label: 'Quotations', path: '/quotations', icon: '▧' },
    { label: 'Approvals', path: '/approvals', icon: '✓' },
    { label: 'Reports', path: '/reports', icon: '▥' },
  ];

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
