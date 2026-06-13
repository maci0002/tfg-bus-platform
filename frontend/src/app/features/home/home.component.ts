import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { TranslateModule } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { TripPlannerComponent } from '../planner/trip-planner.component';
import { TransportService } from '../../core/services/transport.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    TranslateModule,
    TripPlannerComponent,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  private transport = inject(TransportService);

  // Estadísticas reales obtenidas del backend (sustituyen a los valores fijos).
  lineCount = signal<number | null>(null);
  stopCount = signal<number | null>(null);

  features = [
    { icon: 'map',                 key: 'home.features.map' },
    { icon: 'schedule',            key: 'home.features.schedules' },
    { icon: 'confirmation_number', key: 'home.features.booking' },
    { icon: 'notifications',       key: 'home.features.alerts' },
    { icon: 'qr_code',             key: 'home.features.ticket' },
    { icon: 'support_agent',       key: 'home.features.support' },
  ];

  constructor() {
    forkJoin({
      lines: this.transport.getLines(),
      stops: this.transport.getStops(),
    }).subscribe({
      next: ({ lines, stops }) => {
        this.lineCount.set(lines.length);
        this.stopCount.set(stops.length);
      },
      error: () => {
        // Sin datos: se mantienen los marcadores vacíos en la plantilla.
      },
    });
  }
}
