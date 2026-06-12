import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { BookingService } from '../../../core/services/booking.service';
import { BookingFlowService } from '../../../core/services/booking-flow.service';
import { AuthService } from '../../../core/services/auth.service';
import { PricingService } from '../../../core/services/pricing.service';

@Component({
  selector: 'app-booking-confirm',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    TranslateModule,
  ],
  templateUrl: './booking-confirm.component.html',
  styleUrl: './booking-confirm.component.scss',
})
export class BookingConfirmComponent {

  private booking     = inject(BookingService);
  public  bookingFlow = inject(BookingFlowService);
  public  auth        = inject(AuthService);
  private pricing     = inject(PricingService);
  private router      = inject(Router);
  private snack       = inject(MatSnackBar);

  submitting = signal(false);

  constructor() {
    if (!this.bookingFlow.hasTrip() || !this.bookingFlow.hasSeat()) {
      this.router.navigate(['/']);
    }
  }

  get trip()  { return this.bookingFlow.selectedTrip(); }
  get seat()  { return this.bookingFlow.selectedSeat(); }
  get price() { return this.pricing.calculate(this.trip?.durationMinutes ?? 0); }

  formatDuration(m: number): string {
    if (m < 60) return `${m} min`;
    const h = Math.floor(m / 60);
    const min = m % 60;
    return min === 0 ? `${h} h` : `${h} h ${min} min`;
  }

  back(): void {
    this.router.navigate(['/booking/seats']);
  }

  confirm(): void {
    if (!this.auth.isLoggedIn()) {
      this.snack.open('Debes iniciar sesión para reservar', 'Cerrar', { duration: 3500 });
      this.router.navigate(['/login']);
      return;
    }

    const trip = this.trip!;
    const seat = this.seat!;
    this.submitting.set(true);

    this.booking.create({
      lineId: trip.lineId,
      originStopCode: trip.originStopCode,
      destinationStopCode: trip.destinationStopCode,
      travelDate: trip.travelDate,
      departureTime: trip.departureTime,
      seatCode: seat,
    }).subscribe({
      next: reservation => {
        this.bookingFlow.clear();
        this.snack.open('Reserva creada correctamente', 'Cerrar', { duration: 3500 });
        this.router.navigate(['/bookings', reservation.id]);
      },
      error: err => {
        this.submitting.set(false);
        const msg = err?.error?.error ?? 'No se pudo crear la reserva';
        this.snack.open(msg, 'Cerrar', { duration: 4500 });
      },
    });
  }
}
