import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-module-placeholder',
  templateUrl: './module-placeholder.component.html',
  styleUrl: './module-placeholder.component.scss',
})
export class ModulePlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly page = toSignal(this.route.data, {
    initialValue: this.route.snapshot.data,
  });
}
