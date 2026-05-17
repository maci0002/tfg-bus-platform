package com.tfg.busplatform.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSearchResultDto {
    private Long lineId;
    private String lineCode;
    private String lineName;
    private String lineColor;

    private String originStop;
    private String destinationStop;

    private String departureTime;
    private String arrivalTime;
    private Integer durationMinutes;

    private List<LineStopDto> stops;
    /** [[lat, lng], ...] para pintar polyline en el mapa */
    private List<double[]> coordinates;
}
