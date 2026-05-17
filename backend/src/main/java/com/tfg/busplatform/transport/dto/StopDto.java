package com.tfg.busplatform.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopDto {
    private Long id;
    private String code;
    private String name;
    private Double latitude;
    private Double longitude;
}
