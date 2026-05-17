import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { TransportService } from '../../core/services/transport.service';
import { LineSummary } from '../../core/models/transport.model';

@Component({
  selector: 'app-lines',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './lines.component.html',
  styleUrl: './lines.component.scss',
})
export class LinesComponent {
  private transport = inject(TransportService);

  lines = signal<LineSummary[]>([]);
  loading = signal(true);

  constructor() {
    this.transport.getLines().subscribe({
      next: lines => {
        this.lines.set(lines);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  formatDuration(minutes: number): string {
    if (minutes < 60) return `${minutes} min`;
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return m === 0 ? `${h} h` : `${h} h ${m} min`;
  }
}
