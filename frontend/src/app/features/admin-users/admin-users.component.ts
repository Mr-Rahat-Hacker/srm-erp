import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminUser, CreateAdminUser } from './admin-user.models';
import { AdminUserService } from './admin-user.service';

@Component({
  selector: 'app-admin-users',
  imports: [FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss',
})
export class AdminUsersComponent implements OnInit {
  private readonly service = inject(AdminUserService);
  protected users: AdminUser[] = [];
  protected roles = ['SUPER_ADMIN', 'SRM_ADMIN', 'SUPPLIER_VERIFIER', 'PROCUREMENT_BUYER', 'PROCUREMENT_APPROVER', 'AUDITOR'];
  protected rolesDraft: Record<string, string[]> = {};
  protected form: CreateAdminUser = this.emptyForm();
  protected loading = false;
  protected saving = false;
  protected savingUserId: string | null = null;
  protected error = '';
  protected notice = '';

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading = true;
    this.error = '';
    this.service.list().subscribe({
      next: (response) => {
        this.users = response.data ?? [];
        this.rolesDraft = Object.fromEntries(this.users.map((user) => [user.userId, [...user.roles]]));
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.message ?? 'Could not load user accounts.';
        this.loading = false;
      },
    });
  }

  protected toggleRole(role: string, checked: boolean): void {
    this.form.roles = checked
      ? [...new Set([...this.form.roles, role])]
      : this.form.roles.filter((item) => item !== role);
  }

  protected toggleUserRole(userId: string, role: string, checked: boolean): void {
    const current = this.rolesDraft[userId] ?? [];
    this.rolesDraft[userId] = checked
      ? [...new Set([...current, role])]
      : current.filter((item) => item !== role);
  }

  protected rolesChanged(user: AdminUser): boolean {
    return (this.rolesDraft[user.userId] ?? []).slice().sort().join('|') !== user.roles.slice().sort().join('|');
  }

  protected saveRoles(user: AdminUser): void {
    this.error = '';
    this.notice = '';
    this.savingUserId = user.userId;
    this.service.updateRoles(user.userId, this.rolesDraft[user.userId] ?? []).subscribe({
      next: (response) => {
        this.users = this.users.map((item) => item.userId === user.userId ? response.data : item);
        this.rolesDraft[user.userId] = [...response.data.roles];
        this.notice = response.message;
        this.savingUserId = null;
      },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.message ?? 'Could not update user roles.';
        this.savingUserId = null;
      },
    });
  }

  protected create(): void {
    this.error = '';
    this.notice = '';
    this.saving = true;
    this.service.create({ ...this.form, email: this.form.email.trim() }).subscribe({
      next: (response) => {
        this.notice = response.message;
        this.form = this.emptyForm();
        this.saving = false;
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.message ?? 'Could not create this account.';
        this.saving = false;
      },
    });
  }

  protected changeStatus(user: AdminUser, accountStatus: AdminUser['accountStatus']): void {
    this.error = '';
    this.notice = '';
    this.service.updateStatus(user.userId, accountStatus).subscribe({
      next: (response) => {
        this.users = this.users.map((item) => item.userId === user.userId ? response.data : item);
        this.notice = response.message;
      },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.message ?? 'Could not update account status.';
      },
    });
  }

  private emptyForm(): CreateAdminUser {
    return { fullName: '', email: '', password: '', roles: [] };
  }
}
