import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditEvent } from './audit.models';
import { AuditService } from './audit.service';

@Component({
  selector: 'app-audit',
  imports: [DatePipe, FormsModule],
  templateUrl: './audit.component.html',
  styleUrl: './audit.component.scss',
})
export class AuditComponent implements OnInit {
  private readonly audit = inject(AuditService);
  events: AuditEvent[] = [];
  action = '';
  entityType = '';
  rfqId = '';
  actorUserId = '';
  from = '';
  to = '';
  page = 0;
  pageSize = 25;
  totalElements = 0;
  totalPages = 0;
  loading = false;
  error = '';

  ngOnInit(): void { this.loadPage(); }

  search(): void {
    this.page = 0;
    this.loadPage();
  }

  changePage(delta: number): void {
    const next = this.page + delta;
    if (next < 0 || next >= this.totalPages || this.loading) return;
    this.page = next;
    this.loadPage();
  }

  private loadPage(): void {
    this.loading = true;
    this.error = '';
    const filters = {
      action: this.action.trim() || undefined,
      entityType: this.entityType.trim() || undefined,
      rfqId: this.rfqId.trim() || undefined,
      actorUserId: this.actorUserId.trim() || undefined,
      from: this.from ? new Date(this.from).toISOString() : undefined,
      to: this.to ? new Date(this.to).toISOString() : undefined,
      page: this.page,
      size: this.pageSize,
    };
    this.audit.search(filters).subscribe({
      next: (response) => {
        this.events = response.data?.items ?? [];
        this.totalElements = response.data?.totalElements ?? 0;
        this.totalPages = response.data?.totalPages ?? 0;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.message ?? 'Unable to retrieve audit events.';
        this.loading = false;
      },
    });
  }
}
