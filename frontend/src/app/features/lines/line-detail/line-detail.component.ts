import { AfterViewInit, Component, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { TransportService } from '../../../core/services/transport.service';
import { LineDetail } from '../../../core/models/transport.model';

@Component({
  selector: 'app-line-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './line-detail.component.html',
  styleUrl: './line-detail.component.scss',
})
export class LineDetailComponent implements AfterViewInit {
  private route     = inject(ActivatedRoute);
  private transport = inject(TransportService);

  @ViewChild('mapEl') mapEl!: ElementRef<HTMLDivElement>;

  line = signal<LineDetail | null>(null);
  loading = signal(true);
  error = signal(false);

  private mapInitialized = false;

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (Number.isNaN(id)) {
      this.error.set(true);
      this.loading.set(false);
      return;
    }

    this.transport.getLine(id).subscribe({
      next: line => {
        this.line.set(line);
        this.loading.set(false);
        queueMicrotask(() => this.tryRenderMap());
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  ngAfterViewInit(): void {
    this.tryRenderMap();
  }

  private async tryRenderMap(): Promise<void> {
    if (this.mapInitialized) return;
    const line = this.line();
    if (!line || !this.mapEl?.nativeElement || line.stops.length === 0) return;

    this.mapInitialized = true;

    const L = await import('leaflet');
    const coords: [number, number][] = line.stops.map(s => [s.latitude, s.longitude]);

    const map = L.map(this.mapEl.nativeElement, {
      scrollWheelZoom: false,
      preferCanvas: true,
    });

    // Tile claro tipo CartoDB Positron
    L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> ' +
        '&copy; <a href="https://carto.com/attributions">CARTO</a>',
      maxZoom: 18,
      subdomains: 'abcd',
      detectRetina: true,
    }).addTo(map);

    // Halo de fondo para profundidad
    L.polyline(coords, {
      color: line.color,
      weight: 14,
      opacity: 0.15,
      lineCap: 'round',
      lineJoin: 'round',
    }).addTo(map);

    // Polyline principal
    L.polyline(coords, {
      color: line.color,
      weight: 5,
      opacity: 1,
      lineCap: 'round',
      lineJoin: 'round',
    }).addTo(map);

    // Paradas: círculos blancos con borde de color
    line.stops.forEach((s, idx) => {
      const isEndpoint = idx === 0 || idx === line.stops.length - 1;
      L.circleMarker([s.latitude, s.longitude], {
        radius: isEndpoint ? 8 : 6,
        color: line.color,
        fillColor: '#fff',
        fillOpacity: 1,
        weight: isEndpoint ? 4 : 3,
      })
        .bindTooltip(
          `<strong>${s.name}</strong><br><span style="opacity:.7">+${s.minutesFromStart} min</span>`,
          { direction: 'top', offset: [0, -8], opacity: 0.95 },
        )
        .addTo(map);
    });

    map.fitBounds(L.latLngBounds(coords), { padding: [40, 40] });

    // Forzar recálculo de dimensiones tras el render
    setTimeout(() => map.invalidateSize(), 100);
  }

  formatDuration(minutes: number): string {
    if (minutes < 60) return `${minutes} min`;
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return m === 0 ? `${h} h` : `${h} h ${m} min`;
  }
}
