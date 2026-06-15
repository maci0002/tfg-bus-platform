package com.tfg.busplatform.booking.service;

import com.tfg.busplatform.booking.dto.PaymentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de la pasarela de pago simulada: validación de Luhn, caducidad,
 * formato y la regla de simulación que rechaza las tarjetas terminadas en 0000.
 */
class SimulatedPaymentGatewayTest {

    private final SimulatedPaymentGateway gateway = new SimulatedPaymentGateway();

    private PaymentRequest card(String number, int month, int year) {
        return new PaymentRequest("Titular de Prueba", number, month, year, "123");
    }

    @Test
    @DisplayName("Autoriza una tarjeta válida y devuelve los últimos 4 dígitos")
    void approvesValidCard() {
        SimulatedPaymentGateway.PaymentResult result = gateway.authorize(card("4242424242424242", 12, 2099));
        assertThat(result.approved()).isTrue();
        assertThat(result.cardLast4()).isEqualTo("4242");
        assertThat(result.declineReason()).isNull();
    }

    @Test
    @DisplayName("Admite tarjetas con espacios entre los grupos de dígitos")
    void approvesCardWithSpaces() {
        SimulatedPaymentGateway.PaymentResult result = gateway.authorize(card("4242 4242 4242 4242", 12, 2099));
        assertThat(result.approved()).isTrue();
        assertThat(result.cardLast4()).isEqualTo("4242");
    }

    @Test
    @DisplayName("Rechaza un número de tarjeta con formato no válido (pocos dígitos)")
    void declinesBadFormat() {
        SimulatedPaymentGateway.PaymentResult result = gateway.authorize(card("12345", 12, 2099));
        assertThat(result.approved()).isFalse();
        assertThat(result.declineReason()).contains("formato");
    }

    @Test
    @DisplayName("Rechaza un número que no supera el algoritmo de Luhn")
    void declinesLuhnFailure() {
        SimulatedPaymentGateway.PaymentResult result = gateway.authorize(card("4242424242424241", 12, 2099));
        assertThat(result.approved()).isFalse();
        assertThat(result.declineReason()).contains("no es válido");
    }

    @Test
    @DisplayName("Rechaza una tarjeta caducada")
    void declinesExpiredCard() {
        YearMonth past = YearMonth.now().minusMonths(1);
        SimulatedPaymentGateway.PaymentResult result =
                gateway.authorize(card("4242424242424242", past.getMonthValue(), past.getYear()));
        assertThat(result.approved()).isFalse();
        assertThat(result.declineReason()).contains("caducada");
    }

    @Test
    @DisplayName("Simula un rechazo bancario para tarjetas terminadas en 0000")
    void declinesCardEndingIn0000() {
        // 4242424242420000 supera Luhn pero la regla de simulación lo rechaza.
        SimulatedPaymentGateway.PaymentResult result = gateway.authorize(card("4242424242420000", 12, 2099));
        assertThat(result.approved()).isFalse();
        assertThat(result.declineReason()).contains("entidad emisora");
    }
}
