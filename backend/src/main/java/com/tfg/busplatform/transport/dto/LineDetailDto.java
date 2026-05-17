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
public class LineDetailDto {
    private Long id;
    private String code;
    private String name;
    private String color;
    private String originName;
    private String destinationName;
    private Integer durationMinutes;
    private List<String> departureTimes;
    private List<LineStopDto> stops;
}
