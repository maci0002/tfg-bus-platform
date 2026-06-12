package com.tfg.busplatform.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {

    private Long id;
    private String code;

    private Long lineId;
    private String lineCode;
    private String lineName;
    private String lineColor;

    private String originStop;
    private String destinationStop;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate travelDate;

    /** "HH:mm" */
    private String departureTime;
    /** "HH:mm" */
    private String arrivalTime;
    private Integer durationMinutes;

    private String seatCode;
    private String status;
    private BigDecimal price;

    private LocalDateTime createdAt;
}
