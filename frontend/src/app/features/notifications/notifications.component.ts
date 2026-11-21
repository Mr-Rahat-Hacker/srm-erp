import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificationService } from './notification.service';
import { NotificationRecord } from './notification.models';

@Component({
  selector: 'app-notifications',
  imports: [DatePipe, RouterLink],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
})
export class NotificationsComponent implements OnInit {
  private readonly notifications = inject(NotificationService);
  items: NotificationRecord[] = [];
  loading = true;
  error = '';

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.error = '';
    this.notifications.inbox().subscribe({
      next: (response) => { this.items = response.data ?? []; this.loading = false; },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.message ?? 'Unable to load notifications.';
        this.loading = false;
      },
    });
  }

  markRead(item: NotificationRecord): void {
    if (item.readAt) return;
    this.notifications.markRead(item.notificationId).subscribe({
      next: (response) => {
        this.items = this.items.map((current) => current.notificationId === item.notificationId
          ? response.data : current);
      },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.message ?? 'Unable to update this notification.';
      },
    });
  }
}
