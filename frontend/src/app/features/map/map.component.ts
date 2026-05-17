import { AfterViewInit, Component, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { TransportService } from '../../core/services/transport.service';
import { LineDetail, LineSummary, Stop } from '../../core/models/transport.model';

// Tiles claros tipo "carta moderna" — mucho más limpios que OSM estándar
const TILE_URL = 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png';
const TILE_ATTR =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> ' +
  '&copy; <a href="https://carto.com/attributions">CARTO</a>';

// Bounds aproximados del entorno de Jaén
const JAEN_BOUNDS: [[number, number], [number, number]] = [
  [37.55, -4.15],
  [38.15, -2.95],
];

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    TranslateModule,
  ],
  templateUrl: './map.component.html',
  styleUrl: './map.component.scss',
})
export class MapComponent implements AfterViewInit {
  private transport = inject(TransportService);

  @ViewChild('mapEl') mapEl!: ElementRef<HTMLDivElement>;

  lines = signal<LineSummary[]>([]);
  stops = signal<Stop[]>([]);
  selectedLineId = signal<number | null>(null);
  loading = signal(true);

  private map: any;
  private L: any;
  private linesLayer: any;
  private stopsLayer: any;
  private labelsLayer: any;
  private lineCache = new Map<number, LineDetail>();

  constructor() {
    forkJoin({
      lines: this.transport.getLines(),
      stops: this.transport.getStops(),
    }).subscribe(({ lines, stops }) => {
      this.lines.set(lines);
      this.stops.set(stops);
      this.loading.set(false);
      queueMicrotask(() => this.renderMap());
    });
  }

  async ngAfterViewInit(): Promise<void> {
    if (!this.loading()) this.renderMap();
  }

  private async renderMap(): Promise<void> {
    if (this.map || !this.mapEl?.nativeElement) return;

    this.L = await import('leaflet');
    const L = this.L;

    this.map = L.map(this.mapEl.nativeElement, {
      zoomControl: true,
      attributionControl: true,
      preferCanvas: true,
    });

    L.tileLayer(TILE_URL, {
      attribution: TILE_ATTR,
      maxZoom: 18,
      subdomains: 'abcd',
      detectRetina: true,
    }).addTo(this.map);

    this.linesLayer  = L.layerGroup().addTo(this.map);
    this.stopsLayer  = L.layerGroup().addTo(this.map);
    this.labelsLayer = L.layerGroup().addTo(this.map);

    // Encuadrar Jaén
    this.map.fitBounds(JAEN_BOUNDS, { padding: [40, 40] });

    this.renderAllStops();

    // Forzar recálculo de tamaño tras el render (evita "tiles rotos" cuando
    // el contenedor todavía no tenía dimensiones al inicializarse Leaflet)
    setTimeout(() => this.map.invalidateSize(), 100);
  }

  private renderAllStops(): void {
    if (!this.L || !this.stopsLayer) return;
    this.stopsLayer.clearLayers();

    this.stops().forEach(s => {
      this.L.circleMarker([s.latitude, s.longitude], {
        radius: 4,
        color: '#94A3B8',
        fillColor: '#fff',
        fillOpacity: 1,
        weight: 2,
        opacity: 0.85,
      })
        .bindTooltip(s.name, { direction: 'top', offset: [0, -6], opacity: 0.95 })
        .addTo(this.stopsLayer);
    });
  }

  selectLine(line: LineSummary | null): void {
    if (!this.L || !this.map) return;

    this.linesLayer.clearLayers();
    this.labelsLayer.clearLayers();
    this.selectedLineId.set(line?.id ?? null);

    if (!line) {
      this.renderAllStops();
      this.map.flyToBounds(JAEN_BOUNDS, { padding: [40, 40], duration: 0.6 });
      return;
    }

    const draw = (detail: LineDetail) => {
      const coords: [number, number][] = detail.stops.map(s => [s.latitude, s.longitude]);

      // Halo de fondo (anchura mayor, casi transparente) para profundidad
      this.L.polyline(coords, {
        color: detail.color,
        weight: 14,
        opacity: 0.15,
        lineCap: 'round',
        lineJoin: 'round',
      }).addTo(this.linesLayer);

      // Polyline principal
      this.L.polyline(coords, {
        color: detail.color,
        weight: 5,
        opacity: 1,
        lineCap: 'round',
        lineJoin: 'round',
      }).addTo(this.linesLayer);

      // Paradas como círculos blancos con borde de color de línea
      this.stopsLayer.clearLayers();
      detail.stops.forEach((s, idx) => {
        const isEndpoint = idx === 0 || idx === detail.stops.length - 1;
        this.L.circleMarker([s.latitude, s.longitude], {
          radius: isEndpoint ? 8 : 6,
          color: detail.color,
          fillColor: '#fff',
          fillOpacity: 1,
          weight: isEndpoint ? 4 : 3,
        })
          .bindTooltip(
            `<strong>${s.name}</strong><br><span style="opacity:.7">+${s.minutesFromStart} min</span>`,
            { direction: 'top', offset: [0, -8], opacity: 0.95 },
          )
          .addTo(this.stopsLayer);
      });

      // Etiqueta con el código de línea en el primer tramo
      if (coords.length >= 2) {
        const midLat = (coords[0][0] + coords[1][0]) / 2;
        const midLng = (coords[0][1] + coords[1][1]) / 2;
        const labelIcon = this.L.divIcon({
          className: 'map-line-label',
          html: `<span style="background:${detail.color}">${detail.code}</span>`,
          iconSize: [40, 22],
          iconAnchor: [20, 11],
        });
        this.L.marker([midLat, midLng], { icon: labelIcon, interactive: false })
          .addTo(this.labelsLayer);
      }

      this.map.flyToBounds(this.L.latLngBounds(coords), { padding: [50, 50], duration: 0.6 });
    };

    const cached = this.lineCache.get(line.id);
    if (cached) {
      draw(cached);
    } else {
      this.transport.getLine(line.id).subscribe(detail => {
        this.lineCache.set(line.id, detail);
        draw(detail);
      });
    }
  }
}
