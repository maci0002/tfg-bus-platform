package com.tfg.busplatform.transport.ctan;

import com.tfg.busplatform.transport.ctan.dto.CtanHorariosResponse;
import com.tfg.busplatform.transport.ctan.dto.CtanHorariosResponse.CtanServicio;
import com.tfg.busplatform.transport.ctan.dto.CtanLineaParadasResponse.CtanLineaParada;
import com.tfg.busplatform.transport.ctan.dto.CtanLineasResponse.CtanLinea;
import com.tfg.busplatform.transport.ctan.dto.CtanParadasResponse.CtanParada;
import com.tfg.busplatform.transport.model.Line;
import com.tfg.busplatform.transport.model.LineStop;
import com.tfg.busplatform.transport.model.Stop;
import com.tfg.busplatform.transport.repository.LineRepository;
import com.tfg.busplatform.transport.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Sincroniza los datos abiertos de la API de CTAN (consorcio de Jaén) con el
 * modelo de dominio propio ({@link Stop}, {@link Line}, {@link LineStop}).
 *
 * <p>Decisiones de mapeo relevantes:</p>
 * <ul>
 *   <li>Cada parada CTAN se almacena con código {@code CTAN-<idParada>} para
 *       permitir actualizaciones idempotentes (upsert).</li>
 *   <li>El recorrido de la línea se toma del sentido de ida (sentido = 1),
 *       ordenado por el campo {@code orden}.</li>
 *   <li>La API no expone los minutos acumulados por parada, por lo que se
 *       estiman mediante la distancia haversine entre paradas y una velocidad
 *       media configurable.</li>
 *   <li>Las horas de salida se extraen de la primera hora de cada expedición
 *       del horario de ida; si no hay datos, se generan horarios de respaldo.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CtanSyncService {

    private final CtanClient client;
    private final CtanProperties props;
    private final StopRepository stopRepository;
    private final LineRepository lineRepository;

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("H:mm");

    /** Paleta de colores asignada a las líneas (la API de CTAN no expone color). */
    private static final String[] PALETTE = {
            "#1976D2", "#D32F2F", "#388E3C", "#F57C00", "#7B1FA2", "#00897B",
            "#5D4037", "#C2185B", "#0097A7", "#FBC02D", "#512DA8", "#455A64"
    };

    /** Resultado resumido de una sincronización. */
    public record SyncResult(int stops, int lines) {}

    @Transactional
    public SyncResult sync() {
        log.info("CTAN: iniciando sincronización (consorcio {} - {})...",
                props.getConsorcioId(), props.getBaseUrl());

        // 1. Paradas globales del consorcio -> Stop, indexadas por idParada CTAN.
        Map<String, Stop> stopByCtanId = new HashMap<>();
        for (CtanParada p : client.getParadas()) {
            Stop stop = upsertStop(p.idParada(), p.nombre(), p.latitud(), p.longitud());
            if (stop != null) {
                stopByCtanId.put(p.idParada(), stop);
            }
        }
        log.info("CTAN: {} paradas sincronizadas.", stopByCtanId.size());

        // 2. Líneas.
        List<CtanLinea> lineas = client.getLineas();
        int limit = props.getMaxLines() > 0
                ? Math.min(props.getMaxLines(), lineas.size())
                : lineas.size();

        Set<String> usedCodes = new HashSet<>();
        int colorIdx = 0;
        int linesLoaded = 0;

        for (int i = 0; i < limit; i++) {
            CtanLinea l = lineas.get(i);
            try {
                boolean ok = syncLine(l, stopByCtanId, usedCodes, PALETTE[colorIdx % PALETTE.length]);
                if (ok) {
                    linesLoaded++;
                    colorIdx++;
                }
            } catch (Exception e) {
                log.warn("CTAN: error sincronizando línea {} ({}): {}",
                        l.idLinea(), l.codigo(), e.getMessage());
            }
        }

        log.info("CTAN: sincronización completada. {} paradas y {} líneas en base de datos.",
                stopRepository.count(), linesLoaded);
        return new SyncResult((int) stopRepository.count(), linesLoaded);
    }

    // ── Líneas ──────────────────────────────────────────────────────────────

    private boolean syncLine(CtanLinea l, Map<String, Stop> stopByCtanId,
                             Set<String> usedCodes, String color) {

        List<CtanLineaParada> raw = client.getLineaParadas(l.idLinea());
        if (raw.isEmpty()) {
            return false;
        }

        // Recorrido del sentido de ida; si no hay, se usan todas las paradas.
        List<CtanLineaParada> ida = raw.stream()
                .filter(p -> "1".equals(p.sentido()))
                .sorted(Comparator.comparing(p -> p.orden() == null ? 0 : p.orden()))
                .toList();
        if (ida.size() < 2) {
            ida = raw.stream()
                    .sorted(Comparator.comparing(p -> p.orden() == null ? 0 : p.orden()))
                    .toList();
        }

        // Resolver/crear las paradas del recorrido (sin repetir paradas consecutivas).
        List<Stop> orderedStops = new ArrayList<>();
        for (CtanLineaParada p : ida) {
            Stop s = stopByCtanId.get(p.idParada());
            if (s == null) {
                s = upsertStop(p.idParada(), p.nombre(), p.latitud(), p.longitud());
                if (s != null) {
                    stopByCtanId.put(p.idParada(), s);
                }
            }
            if (s != null && (orderedStops.isEmpty()
                    || !orderedStops.get(orderedStops.size() - 1).getId().equals(s.getId()))) {
                orderedStops.add(s);
            }
        }
        if (orderedStops.size() < 2) {
            return false;
        }

        List<Integer> minutes = estimateMinutes(orderedStops);
        List<LocalTime> departures = extractDepartures(l.idLinea());
        if (departures.isEmpty()) {
            departures = fallbackDepartures();
        }

        String code = uniqueCode(l.codigo(), l.idLinea(), usedCodes);
        String name = (l.nombre() != null && !l.nombre().isBlank()) ? l.nombre().trim() : code;

        Optional<Line> existing = lineRepository.findByCode(code);
        Line line;
        if (existing.isPresent()) {
            line = existing.get();
            line.getStops().clear();          // orphanRemoval elimina los antiguos
            line.getDepartureTimes().clear();
        } else {
            line = Line.builder()
                    .code(code)
                    .stops(new ArrayList<>())
                    .departureTimes(new ArrayList<>())
                    .build();
        }
        line.setName(name);
        line.setColor(color);

        for (int i = 0; i < orderedStops.size(); i++) {
            LineStop ls = LineStop.builder()
                    .line(line)
                    .stop(orderedStops.get(i))
                    .sequence(i + 1)
                    .minutesFromStart(minutes.get(i))
                    .build();
            line.getStops().add(ls);
        }
        line.getDepartureTimes().addAll(departures);

        lineRepository.save(line);
        return true;
    }

    // ── Paradas ─────────────────────────────────────────────────────────────

    private Stop upsertStop(String idParada, String nombre, String latStr, String lngStr) {
        Double lat = parseDouble(latStr);
        Double lng = parseDouble(lngStr);
        if (idParada == null || nombre == null || nombre.isBlank() || lat == null || lng == null) {
            return null;
        }
        String code = "CTAN-" + idParada;
        Stop stop = stopRepository.findByCode(code).orElseGet(Stop::new);
        stop.setCode(code);
        stop.setName(nombre.trim());
        stop.setLatitude(lat);
        stop.setLongitude(lng);
        return stopRepository.save(stop);
    }

    // ── Horarios ────────────────────────────────────────────────────────────

    private List<LocalTime> extractDepartures(String idLinea) {
        Set<LocalTime> times = new TreeSet<>();
        CtanHorariosResponse h = client.getHorarios(idLinea, 1);
        if (h != null && h.planificadores() != null) {
            h.planificadores().forEach(p -> collectDepartureTimes(p.horarioIda(), times));
        }
        return new ArrayList<>(times);
    }

    /** Toma la primera hora válida de cada expedición (salida desde la cabecera). */
    private void collectDepartureTimes(List<CtanServicio> servicios, Set<LocalTime> out) {
        if (servicios == null) {
            return;
        }
        for (CtanServicio s : servicios) {
            if (s.horas() == null) {
                continue;
            }
            for (String hora : s.horas()) {
                LocalTime t = parseTime(hora);
                if (t != null) {
                    out.add(t);
                    break; // la primera hora es la salida de cabecera
                }
            }
        }
    }

    private List<LocalTime> fallbackDepartures() {
        List<LocalTime> times = new ArrayList<>();
        for (int hour = 7; hour <= 21; hour += 2) {
            times.add(LocalTime.of(hour, 0));
        }
        return times;
    }

    // ── Estimación de tiempos por distancia ─────────────────────────────────

    private List<Integer> estimateMinutes(List<Stop> stops) {
        List<Integer> minutes = new ArrayList<>();
        minutes.add(0);
        double cumulativeKm = 0;
        for (int i = 1; i < stops.size(); i++) {
            cumulativeKm += haversineKm(stops.get(i - 1), stops.get(i));
            int m = (int) Math.round(cumulativeKm / props.getAverageSpeedKmh() * 60.0);
            if (m <= minutes.get(i - 1)) {
                m = minutes.get(i - 1) + 1; // garantizar tiempos estrictamente crecientes
            }
            minutes.add(m);
        }
        return minutes;
    }

    private double haversineKm(Stop a, Stop b) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLng = Math.toRadians(b.getLongitude() - a.getLongitude());
        double lat1 = Math.toRadians(a.getLatitude());
        double lat2 = Math.toRadians(b.getLatitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLng / 2) * Math.sin(dLng / 2) * Math.cos(lat1) * Math.cos(lat2);
        return 2 * earthRadiusKm * Math.asin(Math.sqrt(h));
    }

    // ── Utilidades de parseo ────────────────────────────────────────────────

    private String uniqueCode(String codigo, String idLinea, Set<String> usedCodes) {
        String base = (codigo != null && !codigo.isBlank()) ? codigo.trim() : "L" + idLinea;
        String code = base;
        if (usedCodes.contains(code)) {
            code = base + "-" + idLinea;
        }
        usedCodes.add(code);
        return code;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim(), HHMM);
        } catch (Exception e) {
            return null;
        }
    }
}
