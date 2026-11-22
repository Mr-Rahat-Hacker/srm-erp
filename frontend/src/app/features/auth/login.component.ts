import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="login-wrap">
      <form class="login-card" (ngSubmit)="submit()">
        <div class="brand-mark">S</div>
        <p class="eyebrow">SUPPLIER RELATIONSHIP MANAGEMENT</p>
        <h1>Sign in to SRM</h1>
        <p class="intro">Use your organization account to access the procurement workspace.</p>
        @if (errorMessage) {
          <div class="error" role="alert">{{ errorMessage }}</div>
        }
        <label>Email address
          <input type="email" name="email" [(ngModel)]="email" required autocomplete="username">
        </label>
        <label>Password
          <input type="password" name="password" [(ngModel)]="password" required autocomplete="current-password">
        </label>
        <button type="submit" [disabled]="loading || !email || !password">
          {{ loading ? 'Signing in…' : 'Sign in' }}
        </button>
        <p class="help">Need a supplier account? <a routerLink="/register">Create one</a>.</p>
      </form>
    </section>
  `,
  styles: [`
    :host { display:block; }
    .login-wrap { min-height:65vh; display:grid; place-items:center; padding:24px; }
    .login-card { width:min(100%,410px); display:grid; gap:15px; padding:34px; border:1px solid #e4eaf0; border-radius:15px; background:#fff; box-shadow:0 14px 40px #20354a0d; }
    .brand-mark { display:grid; place-items:center; width:42px; height:42px; border-radius:11px; background:#183b5d; color:white; font-size:21px; font-weight:800; }
    .eyebrow { margin:6px 0 -10px; color:#8392a2; font-size:9px; font-weight:800; letter-spacing:.13em; }
    h1 { margin:0; color:#1b2c40; font-size:27px; }
    .intro,.help { margin:0; color:#77879a; font-size:12px; line-height:1.6; }
    label { display:grid; gap:7px; color:#4d6075; font-size:11px; font-weight:700; }
    input { box-sizing:border-box; width:100%; min-height:42px; padding:10px 12px; border:1px solid #dce4ec; border-radius:7px; font:inherit; font-size:13px; }
    button { min-height:43px; border:0; border-radius:7px; background:#183b5d; color:white; font-weight:750; cursor:pointer; }
    button:disabled { opacity:.55; cursor:not-allowed; }
    .error { padding:11px; border-radius:7px; background:#fff1f0; color:#a13d36; font-size:12px; }
    .help { text-align:center; font-size:10px; }
  `],
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected email = '';
  protected password = '';
  protected loading = false;
  protected errorMessage = '';

  protected submit(): void {
    this.loading = true;
    this.errorMessage = '';
    this.auth.login({ email: this.email.trim(), password: this.password }).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
        void this.router.navigateByUrl(returnUrl.startsWith('/') ? returnUrl : '/dashboard');
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.status === 401
          ? 'Email ya password incorrect hai, ya account active nahi hai.'
          : error.error?.message || 'Login service unavailable. Backend status check karein.';
        this.loading = false;
      },
    });
  }
}
