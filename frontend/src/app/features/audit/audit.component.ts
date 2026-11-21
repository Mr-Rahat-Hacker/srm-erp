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
  loading = false;
  error = '';

  ngOnInit(): void { this.search(); }

  search(): void {
    this.loading = true;
    this.error = '';
    const filters = {
      action: this.action.trim() || undefined,
      entityType: this.entityType.trim() || undefined,
      rfqId: this.rfqId.trim() || undefined,
      actorUserId: this.actorUserId.trim() || undefined,
      from: this.from ? new Date(this.from).toISOString() : undefined,
      to: this.to ? new Date(this.to).toISOString() : undefined,
      limit: 100,
    };
    this.audit.search(filters).subscribe({
      next: (response) => { this.events = response.data ?? []; this.loading = false; },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.message ?? 'Unable to retrieve audit events.';
        this.loading = false;
      },
    });
  }
}
