package com.tfg.busplatform.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de la fórmula de tarificación del prototipo: 3,00 € de base más
 * 0,12 € por minuto de trayecto, redondeado a dos decimales.
 */
class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    @DisplayName("Un trayecto de 0 minutos cuesta solo la tarifa base")
    void zeroMinutesIsBaseFare() {
        assertThat(pricingService.calculatePrice(0)).isEqualByComparingTo("3.00");
    }

    @ParameterizedTest(name = "{0} min => {1} €")
    @CsvSource({
            "30, 6.60",   // Jaén → Martos
            "60, 10.20",  // Jaén → Úbeda
            "105, 15.60", // Jaén → Cazorla
            "20, 5.40",
            "50, 9.00"
    })
    @DisplayName("La tarifa sigue la fórmula base + 0,12 €/min")
    void followsFormula(int minutes, String expected) {
        assertThat(pricingService.calculatePrice(minutes)).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("El precio siempre tiene exactamente dos decimales")
    void priceHasTwoDecimals() {
        BigDecimal price = pricingService.calculatePrice(37);
        assertThat(price.scale()).isEqualTo(2);
    }
}
