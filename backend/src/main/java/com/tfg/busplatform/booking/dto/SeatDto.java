package com.tfg.busplatform.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDto {
    private String code;       // "5B"
    private int row;           // 5
    private String column;     // "B"
    private boolean occupied;
}
