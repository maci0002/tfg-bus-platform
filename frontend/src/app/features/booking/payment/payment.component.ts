import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { BookingService } from '../../../core/services/booking.service';
import { Reservation } from '../../../core/models/booking.model';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    TranslateModule,
  ],
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.scss',
})
export class PaymentComponent {
  private route     = inject(ActivatedRoute);
  private router    = inject(Router);
  private booking   = inject(BookingService);
  private fb        = inject(FormBuilder);
  private snack     = inject(MatSnackBar);
  private translate = inject(TranslateService);

  reservation = signal<Reservation | null>(null);
  loading = signal(true);
  paying = signal(false);

  /** Tarjeta de prueba precargada (entorno de prototipo, pago simulado). */
  form = this.fb.group({
    cardHolder:  ['VIAJERO DE PRUEBA', [Validators.required]],
    cardNumber:  ['4242 4242 4242 4242', [Validators.required, Validators.pattern(/^[0-9 ]{13,23}$/)]],
    expiryMonth: [12, [Validators.required, Validators.min(1), Validators.max(12)]],
    expiryYear:  [2030, [Validators.required, Validators.min(2024), Validators.max(2099)]],
    cvv:         ['123', [Validators.required, Validators.pattern(/^[0-9]{3,4}$/)]],
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (Number.isNaN(id)) {
      this.router.navigate(['/my-bookings']);
      return;
    }
    this.booking.getById(id).subscribe({
      next: r => {
        // Si la reserva ya no está pendiente, no tiene sentido pagarla.
        if (r.status !== 'PENDING_PAYMENT') {
          this.router.navigate(['/bookings', r.id]);
          return;
        }
        this.reservation.set(r);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/my-bookings']);
      },
    });
  }

  formatDuration(m: number): string {
    if (m < 60) return `${m} min`;
    const h = Math.floor(m / 60);
    const min = m % 60;
    return min === 0 ? `${h} h` : `${h} h ${min} min`;
  }

  back(): void {
    const r = this.reservation();
    this.router.navigate(['/bookings', r ? r.id : '']);
  }

  pay(): void {
    const r = this.reservation();
    if (!r || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.paying.set(true);
    const v = this.form.getRawValue();
    this.booking.pay(r.id, {
      cardHolder:  v.cardHolder!,
      cardNumber:  v.cardNumber!,
      expiryMonth: v.expiryMonth!,
      expiryYear:  v.expiryYear!,
      cvv:         v.cvv!,
    }).subscribe({
      next: updated => {
        this.snack.open(this.translate.instant('payment.success'), 'OK', { duration: 3500 });
        this.router.navigate(['/bookings', updated.id]);
      },
      error: err => {
        this.paying.set(false);
        const msg = err?.error?.error ?? this.translate.instant('payment.declined');
        this.snack.open(msg, 'Cerrar', { duration: 4500 });
      },
    });
  }
}
