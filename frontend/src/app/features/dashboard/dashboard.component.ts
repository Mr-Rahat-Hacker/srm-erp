import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  protected readonly milestones = [
    { name: 'Application foundation', detail: 'Angular workspace and Spring Boot API baseline', complete: true },
    { name: 'Supplier registration', detail: 'Supplier profile, contacts and address management', complete: false },
    { name: 'Document verification', detail: 'Secure documents and internal review workflow', complete: false },
    { name: 'RFQ and quotation', detail: 'Sourcing event and supplier response workflow', complete: false },
  ];
}
