package com.tfg.busplatform.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateReservationRequest {

    @NotNull(message = "La línea es obligatoria")
    private Long lineId;

    @NotBlank(message = "El origen es obligatorio")
    private String originStopCode;

    @NotBlank(message = "El destino es obligatorio")
    private String destinationStopCode;

    @NotNull(message = "La fecha de viaje es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate travelDate;

    @NotNull(message = "La hora de salida es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime departureTime;

    @NotBlank(message = "El asiento es obligatorio")
    private String seatCode;
}
