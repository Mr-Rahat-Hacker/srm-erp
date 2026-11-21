import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly navigation = [
    { label: 'Overview', path: '/dashboard', icon: '⌂' },
    { label: 'Suppliers', path: '/suppliers', icon: '▤' },
    { label: 'RFQ Management', path: '/rfqs', icon: '◎' },
    { label: 'Quotations', path: '/quotations', icon: '▧' },
    { label: 'Approvals', path: '/approvals', icon: '✓' },
    { label: 'Reports', path: '/reports', icon: '▥' },
  ];
}
