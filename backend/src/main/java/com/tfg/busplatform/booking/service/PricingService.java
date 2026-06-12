package com.tfg.busplatform.booking.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo del precio de una reserva.
 *
 * Fórmula del prototipo: base + (minutos × precio_por_minuto).
 * Sirve como simulación de tarifa interurbana.
 *
 *   Jaén → Martos    (30 min) ≈   6,60 €
 *   Jaén → Úbeda     (60 min) ≈  10,20 €
 *   Jaén → Cazorla  (105 min) ≈  15,60 €
 */
@Service
public class PricingService {

    private static final BigDecimal BASE_FARE      = new BigDecimal("3.00");
    private static final BigDecimal PRICE_PER_MIN  = new BigDecimal("0.12");

    public BigDecimal calculatePrice(int durationMinutes) {
        BigDecimal variable = PRICE_PER_MIN.multiply(BigDecimal.valueOf(durationMinutes));
        return BASE_FARE.add(variable).setScale(2, RoundingMode.HALF_UP);
    }
}
