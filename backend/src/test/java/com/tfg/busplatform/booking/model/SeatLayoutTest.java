package com.tfg.busplatform.booking.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del layout fijo del autobús: 10 filas × 4 columnas = 40 plazas.
 */
class SeatLayoutTest {

    @Test
    @DisplayName("El autobús tiene 40 plazas (10 filas × 4 columnas)")
    void totalSeatsIs40() {
        assertThat(SeatLayout.TOTAL_SEATS).isEqualTo(40);
        assertThat(SeatLayout.allSeatCodes()).hasSize(40);
    }

    @Test
    @DisplayName("Los códigos se generan en orden, de 1A a 10D y sin duplicados")
    void seatCodesAreOrderedAndUnique() {
        List<String> codes = SeatLayout.allSeatCodes();
        assertThat(codes.get(0)).isEqualTo("1A");
        assertThat(codes.get(3)).isEqualTo("1D");
        assertThat(codes.get(4)).isEqualTo("2A");
        assertThat(codes.get(39)).isEqualTo("10D");
        assertThat(codes).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1A", "5B", "10D", "1D", "10A"})
    @DisplayName("Acepta códigos de asiento válidos")
    void acceptsValidSeatCodes(String code) {
        assertThat(SeatLayout.isValidSeatCode(code)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0A", "11A", "5E", "A5", "5", "B", "5AA", "99Z"})
    @DisplayName("Rechaza códigos de asiento inválidos")
    void rejectsInvalidSeatCodes(String code) {
        assertThat(SeatLayout.isValidSeatCode(code)).isFalse();
    }

    @Test
    @DisplayName("Rechaza nulo y cadena vacía")
    void rejectsNullAndEmpty() {
        assertThat(SeatLayout.isValidSeatCode(null)).isFalse();
        assertThat(SeatLayout.isValidSeatCode("")).isFalse();
    }
}
