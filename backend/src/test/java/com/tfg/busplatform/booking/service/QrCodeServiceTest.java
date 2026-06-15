package com.tfg.busplatform.booking.service;

import com.tfg.busplatform.booking.model.Reservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del servicio que genera el QR del ticket electrónico (ZXing).
 */
class QrCodeServiceTest {

    private static final String BASE = "http://localhost:8080/api/bookings/verify";

    private final QrCodeService qrCodeService = new QrCodeService(BASE);

    private Reservation reservation(String code, String token) {
        return Reservation.builder().code(code).qrToken(token).build();
    }

    @Test
    @DisplayName("El payload del QR es la URL de verificación con código y token")
    void payloadContainsVerificationUrl() {
        String payload = qrCodeService.buildPayload(reservation("BK-2026-00042", "abc-token-123"));
        assertThat(payload).startsWith(BASE + "?");
        assertThat(payload).contains("code=BK-2026-00042");
        assertThat(payload).contains("token=abc-token-123");
    }

    @Test
    @DisplayName("Cuando no hay token, el parámetro token queda vacío")
    void payloadHandlesNullToken() {
        String payload = qrCodeService.buildPayload(reservation("BK-2026-00099", null));
        assertThat(payload).contains("token=");
        assertThat(payload).doesNotContain("null");
    }

    @Test
    @DisplayName("Genera una imagen PNG no vacía con la cabecera correcta")
    void generatesPngImage() {
        byte[] png = qrCodeService.pngFor(reservation("BK-2026-00042", "abc-token-123"));
        assertThat(png).isNotEmpty();
        // Firma de un fichero PNG: 0x89 'P' 'N' 'G'.
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 'P');
        assertThat(png[2]).isEqualTo((byte) 'N');
        assertThat(png[3]).isEqualTo((byte) 'G');
    }
}
