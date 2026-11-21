import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApprovalService } from './approval.service';
import { ApprovalTask } from './approval.models';

@Component({
  selector: 'app-approvals',
  imports: [DatePipe, FormsModule],
  templateUrl: './approvals.component.html',
  styleUrl: './approvals.component.scss',
})
export class ApprovalsComponent implements OnInit {
  private readonly approvals = inject(ApprovalService);
  tasks: ApprovalTask[] = [];
  comments: Record<string, string> = {};
  loading = true;
  savingTask: string | null = null;
  error = '';
  notice = '';

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.error = '';
    this.approvals.getMyTasks().subscribe({
      next: (response) => { this.tasks = response.data ?? []; this.loading = false; },
      error: (error) => {
        this.error = error?.error?.message ?? 'Could not load assigned approval tasks.';
        this.loading = false;
      },
    });
  }

  decide(task: ApprovalTask, decision: 'APPROVE' | 'REJECT' | 'RETURN_FOR_REVISION'): void {
    const comments = this.comments[task.approvalTaskId]?.trim() ?? '';
    if (!comments) {
      this.error = 'Add a comment before recording your decision.';
      return;
    }
    this.savingTask = task.approvalTaskId;
    this.error = '';
    this.notice = '';
    this.approvals.decide(task.approvalTaskId, decision, comments).subscribe({
      next: (response) => {
        this.notice = response.message;
        this.savingTask = null;
        this.loadTasks();
      },
      error: (error) => {
        this.error = error?.error?.message ?? 'Could not record the approval decision.';
        this.savingTask = null;
      },
    });
  }
}
