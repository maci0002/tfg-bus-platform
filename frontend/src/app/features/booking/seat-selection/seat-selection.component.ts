import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { BookingService } from '../../../core/services/booking.service';
import { BookingFlowService } from '../../../core/services/booking-flow.service';
import { Seat, SeatMap } from '../../../core/models/booking.model';

interface SeatRow {
  row: number;
  left: Seat[];   // A, B
  right: Seat[];  // C, D
}

@Component({
  selector: 'app-seat-selection',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './seat-selection.component.html',
  styleUrl: './seat-selection.component.scss',
})
export class SeatSelectionComponent {

  private booking     = inject(BookingService);
  public  bookingFlow = inject(BookingFlowService);
  private router      = inject(Router);

  seatMap = signal<SeatMap | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  selectedSeat = computed(() => this.bookingFlow.selectedSeat());

  rows = computed<SeatRow[]>(() => {
    const map = this.seatMap();
    if (!map) return [];
    const grouped: SeatRow[] = [];
    for (let r = 1; r <= map.rows; r++) {
      const rowSeats = map.seats.filter(s => s.row === r);
      grouped.push({
        row: r,
        left:  rowSeats.filter(s => s.column === 'A' || s.column === 'B'),
        right: rowSeats.filter(s => s.column === 'C' || s.column === 'D'),
      });
    }
    return grouped;
  });

  availableCount = computed(() => {
    const map = this.seatMap();
    return map ? map.seats.filter(s => !s.occupied).length : 0;
  });

  constructor() {
    const trip = this.bookingFlow.selectedTrip();
    if (!trip) {
      // Si no hay trayecto en el flow, volver al planificador
      this.router.navigate(['/']);
      return;
    }

    this.booking.getSeatMap(
      trip.lineId,
      trip.originStopCode,
      trip.destinationStopCode,
      trip.travelDate,
      trip.departureTime,
    ).subscribe({
      next: map => {
        this.seatMap.set(map);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el mapa de asientos.');
        this.loading.set(false);
      },
    });
  }

  selectSeat(seat: Seat): void {
    if (seat.occupied) return;
    this.bookingFlow.selectSeat(seat.code);
  }

  isSelected(seat: Seat): boolean {
    return this.selectedSeat() === seat.code;
  }

  continueToConfirm(): void {
    if (!this.selectedSeat()) return;
    this.router.navigate(['/booking/confirm']);
  }

  back(): void {
    this.router.navigate(['/']);
  }

  formatDuration(m: number): string {
    if (m < 60) return `${m} min`;
    const h = Math.floor(m / 60);
    const min = m % 60;
    return min === 0 ? `${h} h` : `${h} h ${min} min`;
  }
}
