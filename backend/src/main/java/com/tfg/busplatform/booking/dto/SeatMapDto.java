package com.tfg.busplatform.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapDto {

    private Long lineId;
    private String lineCode;
    private String lineName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate travelDate;
    private String departureTime;     // "HH:mm"
    private String arrivalTime;       // "HH:mm"
    private Integer durationMinutes;

    private String originStop;
    private String destinationStop;
    private BigDecimal price;

    private int rows;
    private List<String> columns;     // ["A","B","C","D"]
    private List<SeatDto> seats;
}
