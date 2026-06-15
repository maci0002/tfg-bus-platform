import { PricingService } from './pricing.service';

/**
 * Pruebas del cálculo de tarifa del cliente, que debe coincidir con el
 * PricingService del backend: base 3,00 € + 0,12 € por minuto.
 */
describe('PricingService', () => {
  let service: PricingService;

  beforeEach(() => {
    service = new PricingService();
  });

  it('un trayecto de 0 minutos cuesta solo la tarifa base', () => {
    expect(service.calculate(0)).toBe(3);
  });

  it.each([
    [30, 6.6],   // Jaén → Martos
    [60, 10.2],  // Jaén → Úbeda
    [105, 15.6], // Jaén → Cazorla
    [20, 5.4],
    [50, 9],
  ])('un trayecto de %i minutos cuesta %f €', (minutes, expected) => {
    expect(service.calculate(minutes)).toBe(expected);
  });

  it('redondea a dos decimales', () => {
    // 37 min → 3 + 4.44 = 7.44
    expect(service.calculate(37)).toBe(7.44);
  });
});
