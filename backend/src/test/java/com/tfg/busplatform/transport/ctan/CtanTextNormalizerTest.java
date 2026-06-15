package com.tfg.busplatform.transport.ctan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de la normalización de los textos crudos de CTAN (nombres de línea y
 * de parada): tipo título con conectores en minúscula, corrección de acentos,
 * eliminación de sufijos de año y de notas operativas.
 */
class CtanTextNormalizerTest {

    @Test
    @DisplayName("Las preposiciones y artículos van en minúscula salvo al inicio del segmento")
    void connectorsStayLowercase() {
        assertThat(CtanTextNormalizer.lineName("Jaén - Sotogordo Por Vados De Torralba"))
                .isEqualTo("Jaén - Sotogordo por Vados de Torralba");
    }

    @Test
    @DisplayName("Corrige los acentos de municipios conocidos del entorno de Jaén")
    void fixesKnownAccents() {
        assertThat(CtanTextNormalizer.lineName("Jaen - Ubeda")).isEqualTo("Jaén - Úbeda");
        assertThat(CtanTextNormalizer.stopName("MENGIBAR")).isEqualTo("Mengíbar");
    }

    @Test
    @DisplayName("Elimina el sufijo de año del nombre de la línea")
    void stripsYearSuffix() {
        assertThat(CtanTextNormalizer.lineName("Jaén - Martos (2026)")).isEqualTo("Jaén - Martos");
    }

    @Test
    @DisplayName("Normaliza el espaciado alrededor de los guiones separadores")
    void normalizesDashSpacing() {
        assertThat(CtanTextNormalizer.lineName("Jaen-Martos")).isEqualTo("Jaén - Martos");
    }

    @Test
    @DisplayName("Elimina las notas operativas embebidas en el nombre de parada")
    void stripsOperationalNotes() {
        String result = CtanTextNormalizer.stopName("Hospital Neurotrau. Solo Bajada (8 a 10 y 15 a 16)");
        assertThat(result).startsWith("Hospital");
        assertThat(result).doesNotContainIgnoringCase("bajada");
    }

    @Test
    @DisplayName("Elimina el sufijo de sentido de una sola letra de la parada")
    void stripsSingleLetterDirectionSuffix() {
        assertThat(CtanTextNormalizer.stopName("Campus Universitario-V")).isEqualTo("Campus Universitario");
    }

    @Test
    @DisplayName("Devuelve la entrada tal cual cuando es nula o está en blanco")
    void returnsInputWhenNullOrBlank() {
        assertThat(CtanTextNormalizer.lineName(null)).isNull();
        assertThat(CtanTextNormalizer.stopName("   ")).isEqualTo("   ");
    }
}
