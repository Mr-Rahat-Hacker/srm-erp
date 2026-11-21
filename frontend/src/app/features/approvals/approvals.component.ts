import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApprovalService } from './approval.service';
import { ApprovalReviewContext, ApprovalTask } from './approval.models';

@Component({
  selector: 'app-approvals',
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './approvals.component.html',
  styleUrl: './approvals.component.scss',
})
export class ApprovalsComponent implements OnInit {
  private readonly approvals = inject(ApprovalService);
  tasks: ApprovalTask[] = [];
  contexts: Record<string, ApprovalReviewContext> = {};
  expandedContext: Record<string, boolean> = {};
  contextLoadingTask: string | null = null;
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

  toggleReviewContext(taskId: string): void {
    if (this.contexts[taskId]) {
      this.expandedContext[taskId] = !this.expandedContext[taskId];
      return;
    }
    this.contextLoadingTask = taskId;
    this.error = '';
    this.approvals.getReviewContext(taskId).subscribe({
      next: (response) => {
        this.contexts[taskId] = response.data;
        this.expandedContext[taskId] = true;
        this.contextLoadingTask = null;
      },
      error: (error) => {
        this.error = error?.error?.message ?? 'Could not load RFQ review context.';
        this.contextLoadingTask = null;
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
