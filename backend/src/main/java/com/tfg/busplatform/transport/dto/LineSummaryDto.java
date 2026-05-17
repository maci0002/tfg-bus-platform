package com.tfg.busplatform.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineSummaryDto {
    private Long id;
    private String code;
    private String name;
    private String color;
    private String originName;
    private String destinationName;
    private Integer totalStops;
    private Integer durationMinutes;
}
