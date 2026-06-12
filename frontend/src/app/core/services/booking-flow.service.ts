import { Injectable, signal, computed } from '@angular/core';
import { SelectedTrip } from '../models/booking.model';

const STORAGE_KEY = 'bp_pending_trip';
const SEAT_KEY    = 'bp_pending_seat';

/**
 * Mantiene el trayecto y el asiento seleccionados durante el flujo de reserva
 * (planificador → mapa de asientos → confirmación). Usa sessionStorage para
 * sobrevivir a recargas pero limpiarse al cerrar el navegador.
 */
@Injectable({ providedIn: 'root' })
export class BookingFlowService {

  private _selectedTrip = signal<SelectedTrip | null>(this.loadTrip());
  private _selectedSeat = signal<string | null>(this.loadSeat());

  readonly selectedTrip = this._selectedTrip.asReadonly();
  readonly selectedSeat = this._selectedSeat.asReadonly();
  readonly hasTrip = computed(() => !!this._selectedTrip());
  readonly hasSeat = computed(() => !!this._selectedSeat());

  selectTrip(trip: SelectedTrip): void {
    this._selectedTrip.set(trip);
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(trip));
    // Al cambiar de trayecto se invalida el asiento previo
    this._selectedSeat.set(null);
    sessionStorage.removeItem(SEAT_KEY);
  }

  selectSeat(seatCode: string): void {
    this._selectedSeat.set(seatCode);
    sessionStorage.setItem(SEAT_KEY, seatCode);
  }

  clear(): void {
    this._selectedTrip.set(null);
    this._selectedSeat.set(null);
    sessionStorage.removeItem(STORAGE_KEY);
    sessionStorage.removeItem(SEAT_KEY);
  }

  private loadTrip(): SelectedTrip | null {
    if (typeof sessionStorage === 'undefined') return null;
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  private loadSeat(): string | null {
    if (typeof sessionStorage === 'undefined') return null;
    return sessionStorage.getItem(SEAT_KEY);
  }
}
