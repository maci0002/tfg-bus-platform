import { Injectable } from '@angular/core';

const BASE_FARE     = 3;
const PRICE_PER_MIN = 0.12;

/**
 * Mismo cálculo que el `PricingService` del backend.
 * Sirve para mostrar el precio en pantalla sin tener que llamar al backend.
 * Se mantienen sincronizados manualmente — si cambia uno, cambia el otro.
 */
@Injectable({ providedIn: 'root' })
export class PricingService {
  calculate(durationMinutes: number): number {
    return Math.round((BASE_FARE + durationMinutes * PRICE_PER_MIN) * 100) / 100;
  }
}
