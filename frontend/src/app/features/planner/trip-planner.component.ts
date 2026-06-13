import { Component, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { combineLatest, debounceTime, distinctUntilChanged, map, Observable, startWith } from 'rxjs';
import { TransportService } from '../../core/services/transport.service';
import { Stop, TripSearchResult } from '../../core/models/transport.model';
import { BookingFlowService } from '../../core/services/booking-flow.service';

@Component({
  selector: 'app-trip-planner',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatAutocompleteModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './trip-planner.component.html',
  styleUrl: './trip-planner.component.scss',
})
export class TripPlannerComponent {
  private fb           = inject(FormBuilder);
  private transport    = inject(TransportService);
  private bookingFlow  = inject(BookingFlowService);
  private router       = inject(Router);

  stops = signal<Stop[]>([]);
  results = signal<TripSearchResult[]>([]);
  loading = signal(false);
  searched = signal(false);
  error = signal<string | null>(null);

  /**
   * Destinos alcanzables desde el origen seleccionado.
   *  - `null`  → todavía no hay un origen válido elegido (destino deshabilitado).
   *  - `Stop[]`→ lista filtrada que se ofrece en el autocompletado de destino.
   */
  reachableDestinations = signal<Stop[] | null>(null);
  private reachable$ = toObservable(this.reachableDestinations);

  form: FormGroup = this.fb.group({
    origin: ['', Validators.required],
    // El destino arranca deshabilitado hasta elegir un origen válido.
    destination: [{ value: '', disabled: true }, Validators.required],
    date: [new Date()],
    time: ['08:00'],
  });

  filteredOrigin!:      Observable<Stop[]>;
  filteredDestination!: Observable<Stop[]>;

  constructor() {
    this.transport.getStops().subscribe(stops => {
      this.stops.set(stops);
      this.setupAutocomplete();
    });
  }

  private setupAutocomplete(): void {
    const originCtrl = this.form.controls['origin'];
    const destCtrl   = this.form.controls['destination'];

    // Origen: autocompletado contra todas las paradas.
    this.filteredOrigin = originCtrl.valueChanges.pipe(
      startWith(''),
      map(value => this.filterStops(this.stops(), value ?? '')),
    );

    // Destino: autocompletado contra los destinos alcanzables (o todas las
    // paradas mientras no haya origen). Se recombina cuando cambia cualquiera
    // de las dos fuentes (texto tecleado o nueva lista de destinos).
    this.filteredDestination = combineLatest([
      destCtrl.valueChanges.pipe(startWith('')),
      this.reachable$,
    ]).pipe(
      map(([value, pool]) => this.filterStops(pool ?? this.stops(), value ?? '')),
    );

    // Al cambiar el origen, recalculamos los destinos posibles.
    originCtrl.valueChanges.pipe(
      debounceTime(200),
      distinctUntilChanged(),
    ).subscribe(value => this.updateDestinations(value ?? ''));
  }

  /** Recalcula los destinos alcanzables según el origen tecleado/seleccionado. */
  private updateDestinations(originValue: string): void {
    const destCtrl = this.form.controls['destination'];
    const match = this.stops().find(
      s => s.name.toLowerCase() === originValue.toLowerCase().trim(),
    );

    if (!match) {
      // Origen aún no válido: se bloquea y se vacía el destino.
      this.reachableDestinations.set(null);
      destCtrl.reset({ value: '', disabled: true });
      return;
    }

    this.transport.getDestinations(match.name).subscribe(destinations => {
      this.reachableDestinations.set(destinations);
      destCtrl.enable({ emitEvent: false });

      // Si el destino actual ya no es alcanzable desde el nuevo origen, se limpia.
      const current = (destCtrl.value ?? '').toLowerCase().trim();
      if (current && !destinations.some(d => d.name.toLowerCase() === current)) {
        destCtrl.setValue('', { emitEvent: false });
      }
    });
  }

  /**
   * Indica si conviene mostrar el municipio junto al nombre de la parada.
   * Se omite cuando no hay municipio o cuando el nombre ya lo contiene
   * (evita redundancias del tipo "Úbeda · Úbeda").
   */
  showMunicipality(s: Stop): boolean {
    if (!s.municipality) return false;
    return !s.name.toLowerCase().includes(s.municipality.toLowerCase());
  }

  private filterStops(pool: Stop[], query: string): Stop[] {
    const q = query.toLowerCase().trim();
    if (!q) return pool;
    return pool.filter(s => s.name.toLowerCase().includes(q));
  }

  swap(): void {
    const { origin, destination } = this.form.getRawValue();
    // Al fijar primero el origen, updateDestinations recalcula y habilita el destino.
    this.form.patchValue({ origin: destination });
    this.form.controls['destination'].setValue(origin);
  }

  search(): void {
    const { origin, destination, time } = this.form.getRawValue();
    if (!origin || !destination) return;

    this.loading.set(true);
    this.error.set(null);
    this.searched.set(true);

    this.transport.searchTrips({ origin, destination, time }).subscribe({
      next: trips => {
        this.results.set(trips);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los resultados.');
        this.loading.set(false);
      },
    });
  }

  formatMinutes(m: number): string {
    if (m < 60) return `${m} min`;
    const h = Math.floor(m / 60);
    const min = m % 60;
    return min === 0 ? `${h} h` : `${h} h ${min} min`;
  }

  /** Guarda el trayecto en el flujo de reserva y navega a la selección de asiento. */
  reserve(trip: TripSearchResult): void {
    const stops = trip.stops;
    const originStopCode      = stops[0]?.code ?? '';
    const destinationStopCode = stops[stops.length - 1]?.code ?? '';
    const date: Date = this.form.value.date instanceof Date
      ? this.form.value.date
      : new Date(this.form.value.date);
    const travelDate = this.toISODate(date);

    this.bookingFlow.selectTrip({
      lineId: trip.lineId,
      lineCode: trip.lineCode,
      lineName: trip.lineName,
      lineColor: trip.lineColor,
      originStopName: trip.originStop,
      originStopCode,
      destinationStopName: trip.destinationStop,
      destinationStopCode,
      travelDate,
      departureTime: trip.departureTime,
      arrivalTime: trip.arrivalTime,
      durationMinutes: trip.durationMinutes,
    });

    this.router.navigate(['/booking/seats']);
  }

  private toISODate(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}
