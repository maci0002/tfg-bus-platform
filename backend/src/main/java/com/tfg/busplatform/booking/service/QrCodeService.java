package com.tfg.busplatform.booking.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.tfg.busplatform.booking.model.Reservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Genera el código QR del ticket electrónico mediante ZXing.
 *
 * <p>El QR codifica la URL pública de verificación del billete con su código y un
 * token aleatorio ({@code .../bookings/verify?code=...&token=...}). Así, al
 * escanearlo, se puede comprobar la validez del ticket sin exponer datos
 * sensibles del usuario ni de la tarjeta.</p>
 */
@Service
@Slf4j
public class QrCodeService {

    private final String verifyBaseUrl;

    public QrCodeService(@Value("${app.ticket.verify-base-url}") String verifyBaseUrl) {
        this.verifyBaseUrl = verifyBaseUrl;
    }

    /** Construye el contenido textual (URL de verificación) que se codifica en el QR. */
    public String buildPayload(Reservation reservation) {
        String code  = URLEncoder.encode(reservation.getCode(), StandardCharsets.UTF_8);
        String token = URLEncoder.encode(
                reservation.getQrToken() == null ? "" : reservation.getQrToken(), StandardCharsets.UTF_8);
        return verifyBaseUrl + "?code=" + code + "&token=" + token;
    }

    /** Renderiza el QR del ticket como imagen PNG. */
    public byte[] pngFor(Reservation reservation) {
        return toPng(buildPayload(reservation), 320);
    }

    private byte[] toPng(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());

            BitMatrix matrix = new QRCodeWriter()
                    .encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("No se pudo generar el QR del ticket: {}", e.getMessage());
            throw new IllegalStateException("Error generando el código QR del ticket", e);
        }
    }
}
