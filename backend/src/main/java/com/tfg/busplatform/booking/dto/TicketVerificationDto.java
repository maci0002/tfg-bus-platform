package com.tfg.busplatform.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Resultado de validar un ticket a partir del contenido de su código QR.
 *
 * <p>Es la respuesta del endpoint público de verificación: indica si el billete
 * es válido (existe, el token coincide y está confirmado) y, en caso afirmativo,
 * los datos mínimos del viaje para mostrarlos a quien lo valida.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketVerificationDto {

    private boolean valid;
    /** Motivo cuando no es válido (no encontrado, token incorrecto, no pagado, cancelado). */
    private String reason;

    private String code;
    private String status;

    private String lineCode;
    private String lineName;
    private String originStop;
    private String destinationStop;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate travelDate;
    private String departureTime;
    private String seatCode;
}
