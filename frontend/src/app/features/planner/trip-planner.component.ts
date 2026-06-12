import { Component, inject, signal } from '@angular/core';
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
import { map, Observable, startWith } from 'rxjs';
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

  form: FormGroup = this.fb.group({
    origin: ['', Validators.required],
    destination: ['', Validators.required],
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
    this.filteredOrigin = this.form.controls['origin'].valueChanges.pipe(
      startWith(''),
      map(value => this.filterStops(value ?? '')),
    );
    this.filteredDestination = this.form.controls['destination'].valueChanges.pipe(
      startWith(''),
      map(value => this.filterStops(value ?? '')),
    );
  }

  private filterStops(query: string): Stop[] {
    const q = query.toLowerCase().trim();
    if (!q) return this.stops();
    return this.stops().filter(s => s.name.toLowerCase().includes(q));
  }

  swap(): void {
    const o = this.form.value.origin;
    const d = this.form.value.destination;
    this.form.patchValue({ origin: d, destination: o });
  }

  search(): void {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);
    this.searched.set(true);

    const { origin, destination, time } = this.form.value;
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
