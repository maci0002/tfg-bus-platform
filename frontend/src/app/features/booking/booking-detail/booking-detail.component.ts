import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { BookingService } from '../../../core/services/booking.service';
import { Reservation } from '../../../core/models/booking.model';

@Component({
  selector: 'app-booking-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    TranslateModule,
  ],
  templateUrl: './booking-detail.component.html',
  styleUrl: './booking-detail.component.scss',
})
export class BookingDetailComponent {
  private route       = inject(ActivatedRoute);
  private router      = inject(Router);
  private booking     = inject(BookingService);
  private snack       = inject(MatSnackBar);
  private translate   = inject(TranslateService);

  reservation = signal<Reservation | null>(null);
  loading = signal(true);
  cancelling = signal(false);

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (Number.isNaN(id)) {
      this.router.navigate(['/my-bookings']);
      return;
    }
    this.booking.getById(id).subscribe({
      next: r => { this.reservation.set(r); this.loading.set(false); },
      error: () => { this.loading.set(false); },
    });
  }

  formatDuration(m: number): string {
    if (m < 60) return `${m} min`;
    const h = Math.floor(m / 60);
    const min = m % 60;
    return min === 0 ? `${h} h` : `${h} h ${min} min`;
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING_PAYMENT: 'booking.status.pendingPayment',
      CONFIRMED:       'booking.status.confirmed',
      CANCELLED:       'booking.status.cancelled',
    };
    return map[status] ?? status;
  }

  statusClass(status: string): string {
    return `status-${status.toLowerCase().replace('_', '-')}`;
  }

  cancelReservation(): void {
    const r = this.reservation();
    if (!r || r.status === 'CANCELLED') return;
    if (!confirm(this.translate.instant('bookingDetail.cancelConfirm'))) return;

    this.cancelling.set(true);
    this.booking.cancel(r.id).subscribe({
      next: updated => {
        this.reservation.set(updated);
        this.cancelling.set(false);
        this.snack.open(this.translate.instant('bookingDetail.cancelled'), 'Cerrar', { duration: 3000 });
      },
      error: () => {
        this.cancelling.set(false);
        this.snack.open('No se pudo cancelar la reserva', 'Cerrar', { duration: 3500 });
      },
    });
  }
}
