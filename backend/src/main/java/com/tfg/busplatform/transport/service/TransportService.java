package com.tfg.busplatform.transport.service;

import com.tfg.busplatform.transport.dto.*;
import com.tfg.busplatform.transport.model.Line;
import com.tfg.busplatform.transport.model.LineStop;
import com.tfg.busplatform.transport.model.Stop;
import com.tfg.busplatform.transport.repository.LineRepository;
import com.tfg.busplatform.transport.repository.StopRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransportService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final LineRepository lineRepository;
    private final StopRepository stopRepository;

    // ── Líneas ───────────────────────────────────────────────────────────────

    public List<LineSummaryDto> getAllLines() {
        return lineRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    public LineDetailDto getLineById(Long id) {
        Line line = lineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Línea no encontrada: " + id));
        return toDetail(line);
    }

    // ── Paradas ──────────────────────────────────────────────────────────────

    public List<StopDto> getAllStops() {
        return stopRepository.findAll().stream()
                .map(this::toStopDto)
                .sorted(Comparator.comparing(StopDto::getName))
                .toList();
    }

    /**
     * Devuelve solo las paradas alcanzables como destino desde el origen dado:
     * aquellas que, en alguna línea que pase por el origen, están aguas abajo
     * (secuencia mayor). Permite que el planificador ofrezca destinos coherentes.
     */
    public List<StopDto> getReachableDestinations(String origin) {
        if (origin == null || origin.isBlank()) return getAllStops();

        Map<Long, Stop> reachable = new LinkedHashMap<>();
        for (Line line : lineRepository.findAll()) {
            Optional<LineStop> originStop = findStopByQuery(line, origin);
            if (originStop.isEmpty()) continue;

            int originSeq = originStop.get().getSequence();
            line.getStops().stream()
                    .filter(ls -> ls.getSequence() > originSeq)
                    .forEach(ls -> reachable.putIfAbsent(ls.getStop().getId(), ls.getStop()));
        }

        return reachable.values().stream()
                .map(this::toStopDto)
                .sorted(Comparator.comparing(StopDto::getName))
                .toList();
    }

    // ── Búsqueda origen-destino ──────────────────────────────────────────────

    public List<TripSearchResultDto> searchTrips(String origin, String destination, LocalTime time) {
        LocalTime fromTime = time != null ? time : LocalTime.MIDNIGHT;

        List<TripSearchResultDto> results = new ArrayList<>();

        for (Line line : lineRepository.findAll()) {
            Optional<LineStop> originStop      = findStopByQuery(line, origin);
            Optional<LineStop> destinationStop = findStopByQuery(line, destination);

            if (originStop.isEmpty() || destinationStop.isEmpty()) continue;
            if (originStop.get().getSequence() >= destinationStop.get().getSequence()) continue;

            int legMinutes = destinationStop.get().getMinutesFromStart()
                    - originStop.get().getMinutesFromStart();

            // Para cada salida, calcular hora de paso por la parada origen
            for (LocalTime departure : line.getDepartureTimes()) {
                LocalTime departureAtOrigin = departure.plusMinutes(originStop.get().getMinutesFromStart());
                if (departureAtOrigin.isBefore(fromTime)) continue;

                LocalTime arrival = departureAtOrigin.plusMinutes(legMinutes);

                results.add(buildSearchResult(line, originStop.get(), destinationStop.get(),
                        departureAtOrigin, arrival, legMinutes));
            }
        }

        // Ordenar por hora de salida ascendente
        results.sort(Comparator.comparing(TripSearchResultDto::getDepartureTime));
        return results;
    }

    // ── Helpers de búsqueda ──────────────────────────────────────────────────

    private Optional<LineStop> findStopByQuery(Line line, String query) {
        if (query == null || query.isBlank()) return Optional.empty();
        String q = query.trim().toLowerCase();
        return line.getStops().stream()
                .filter(ls -> ls.getStop().getName().toLowerCase().contains(q)
                           || ls.getStop().getCode().equalsIgnoreCase(query.trim()))
                .findFirst();
    }

    private TripSearchResultDto buildSearchResult(Line line, LineStop origin, LineStop destination,
                                                  LocalTime departure, LocalTime arrival, int duration) {
        List<LineStopDto> segment = line.getStops().stream()
                .filter(ls -> ls.getSequence() >= origin.getSequence()
                           && ls.getSequence() <= destination.getSequence())
                .map(this::toLineStopDto)
                .toList();

        List<double[]> coords = segment.stream()
                .map(s -> new double[]{ s.getLatitude(), s.getLongitude() })
                .toList();

        return TripSearchResultDto.builder()
                .lineId(line.getId())
                .lineCode(line.getCode())
                .lineName(line.getName())
                .lineColor(line.getColor())
                .originStop(origin.getStop().getName())
                .destinationStop(destination.getStop().getName())
                .departureTime(departure.format(HHMM))
                .arrivalTime(arrival.format(HHMM))
                .durationMinutes(duration)
                .stops(segment)
                .coordinates(coords)
                .build();
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private LineSummaryDto toSummary(Line line) {
        List<LineStop> ordered = line.getStops();
        String originName      = ordered.isEmpty() ? "" : ordered.get(0).getStop().getName();
        String destinationName = ordered.isEmpty() ? "" : ordered.get(ordered.size() - 1).getStop().getName();
        int duration           = ordered.isEmpty() ? 0 : ordered.get(ordered.size() - 1).getMinutesFromStart();

        return LineSummaryDto.builder()
                .id(line.getId())
                .code(line.getCode())
                .name(line.getName())
                .color(line.getColor())
                .originName(originName)
                .destinationName(destinationName)
                .totalStops(ordered.size())
                .durationMinutes(duration)
                .build();
    }

    private LineDetailDto toDetail(Line line) {
        List<LineStop> ordered = line.getStops();
        String originName      = ordered.isEmpty() ? "" : ordered.get(0).getStop().getName();
        String destinationName = ordered.isEmpty() ? "" : ordered.get(ordered.size() - 1).getStop().getName();
        int duration           = ordered.isEmpty() ? 0 : ordered.get(ordered.size() - 1).getMinutesFromStart();

        return LineDetailDto.builder()
                .id(line.getId())
                .code(line.getCode())
                .name(line.getName())
                .color(line.getColor())
                .originName(originName)
                .destinationName(destinationName)
                .durationMinutes(duration)
                .departureTimes(line.getDepartureTimes().stream().map(t -> t.format(HHMM)).toList())
                .stops(ordered.stream().map(this::toLineStopDto).toList())
                .build();
    }

    private LineStopDto toLineStopDto(LineStop ls) {
        Stop s = ls.getStop();
        return LineStopDto.builder()
                .stopId(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .sequence(ls.getSequence())
                .minutesFromStart(ls.getMinutesFromStart())
                .build();
    }

    private StopDto toStopDto(Stop s) {
        return StopDto.builder()
                .id(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .municipality(s.getMunicipality())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .build();
    }
}
