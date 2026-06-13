package com.tfg.busplatform.transport.controller;

import com.tfg.busplatform.transport.dto.LineDetailDto;
import com.tfg.busplatform.transport.dto.LineSummaryDto;
import com.tfg.busplatform.transport.dto.StopDto;
import com.tfg.busplatform.transport.dto.TripSearchResultDto;
import com.tfg.busplatform.transport.service.TransportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/transport")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    @GetMapping("/lines")
    public ResponseEntity<List<LineSummaryDto>> getAllLines() {
        return ResponseEntity.ok(transportService.getAllLines());
    }

    @GetMapping("/lines/{id}")
    public ResponseEntity<LineDetailDto> getLineById(@PathVariable Long id) {
        return ResponseEntity.ok(transportService.getLineById(id));
    }

    @GetMapping("/stops")
    public ResponseEntity<List<StopDto>> getAllStops() {
        return ResponseEntity.ok(transportService.getAllStops());
    }

    /** Paradas alcanzables como destino desde un origen dado (búsqueda dependiente). */
    @GetMapping("/destinations")
    public ResponseEntity<List<StopDto>> getReachableDestinations(@RequestParam String origin) {
        return ResponseEntity.ok(transportService.getReachableDestinations(origin));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TripSearchResultDto>> searchTrips(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) String date,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm") LocalTime time
    ) {
        // `date` se acepta para futura compatibilidad pero no afecta al prototipo
        // (los horarios son los mismos cada día).
        return ResponseEntity.ok(transportService.searchTrips(origin, destination, time));
    }
}
