package com.tfg.busplatform.transport.gtfs;

import com.tfg.busplatform.transport.ctan.CtanTextNormalizer;
import com.tfg.busplatform.transport.gtfs.GtfsDataset.GtfsRoute;
import com.tfg.busplatform.transport.gtfs.GtfsDataset.GtfsStop;
import com.tfg.busplatform.transport.gtfs.GtfsDataset.GtfsStopTime;
import com.tfg.busplatform.transport.gtfs.GtfsDataset.GtfsTrip;
import com.tfg.busplatform.transport.model.Line;
import com.tfg.busplatform.transport.model.LineStop;
import com.tfg.busplatform.transport.model.Stop;
import com.tfg.busplatform.transport.repository.LineRepository;
import com.tfg.busplatform.transport.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Importa el feed GTFS de CTAN (agencia de Jaén) al modelo de dominio propio.
 *
 * <p>Frente a la API REST, el GTFS aporta <strong>horarios reales</strong>
 * ({@code stop_times}) y <strong>colores oficiales</strong> de línea. Para cada
 * línea se elige como recorrido canónico la expedición con más paradas y los
 * tiempos entre paradas se calculan a partir de las horas reales.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GtfsSyncService {

    private final GtfsDownloader downloader;
    private final GtfsParser parser;
    private final GtfsProperties props;
    private final StopRepository stopRepository;
    private final LineRepository lineRepository;

    /** Paleta de respaldo cuando el GTFS no trae color de línea. */
    private static final String[] PALETTE = {
            "#1976D2", "#D32F2F", "#388E3C", "#F57C00", "#7B1FA2", "#00897B",
            "#5D4037", "#C2185B", "#0097A7", "#FBC02D", "#512DA8", "#455A64"
    };

    /** Resultado resumido de una importación. */
    public record SyncResult(int stops, int lines) {}

    @Transactional
    public SyncResult sync() throws Exception {
        Path zip = null;
        try {
            zip = downloader.download();
            GtfsDataset data = parser.parse(zip, props.getAgencyId());

            int colorIdx = 0;
            int linesLoaded = 0;
            Set<String> usedCodes = new HashSet<>();

            for (GtfsRoute route : data.routes().values()) {
                try {
                    boolean ok = syncRoute(route, data, usedCodes,
                            PALETTE[colorIdx % PALETTE.length]);
                    if (ok) {
                        linesLoaded++;
                        colorIdx++;
                    }
                } catch (Exception e) {
                    log.warn("GTFS: error importando ruta {} ({}): {}",
                            route.routeId(), route.shortName(), e.getMessage());
                }
            }

            log.info("GTFS: importación completada. {} paradas y {} líneas en base de datos.",
                    stopRepository.count(), linesLoaded);
            return new SyncResult((int) stopRepository.count(), linesLoaded);
        } finally {
            if (zip != null) {
                Files.deleteIfExists(zip);
            }
        }
    }

    // ── Mapeo de una línea ──────────────────────────────────────────────────

    private boolean syncRoute(GtfsRoute route, GtfsDataset data,
                              Set<String> usedCodes, String fallbackColor) {

        // Expediciones de la ruta.
        List<GtfsTrip> trips = data.trips().values().stream()
                .filter(t -> route.routeId().equals(t.routeId()))
                .toList();
        if (trips.isEmpty()) {
            return false;
        }

        // Recorrido canónico = expedición con más paradas.
        GtfsTrip representative = null;
        int maxStops = 0;
        for (GtfsTrip t : trips) {
            List<GtfsStopTime> st = data.stopTimesByTrip().get(t.tripId());
            int size = st == null ? 0 : st.size();
            if (size > maxStops) {
                maxStops = size;
                representative = t;
            }
        }
        if (representative == null) {
            return false;
        }
        String direction = representative.directionId();
        List<GtfsStopTime> canonical = data.stopTimesByTrip().get(representative.tripId());

        // Resolver paradas del recorrido (sin repetir consecutivas).
        List<Stop> orderedStops = new ArrayList<>();
        List<Integer> departureSeconds = new ArrayList<>();
        for (GtfsStopTime st : canonical) {
            GtfsStop gs = data.stops().get(st.stopId());
            if (gs == null) {
                continue;
            }
            Stop s = upsertStop(gs);
            if (s == null) {
                continue;
            }
            if (orderedStops.isEmpty()
                    || !orderedStops.get(orderedStops.size() - 1).getId().equals(s.getId())) {
                orderedStops.add(s);
                departureSeconds.add(st.departureSeconds());
            }
        }
        if (orderedStops.size() < Math.max(2, props.getMinStops())) {
            return false;
        }

        List<Integer> minutes = minutesFromStart(departureSeconds);
        List<LocalTime> departures = collectDepartures(trips, direction, data);
        if (departures.isEmpty()) {
            departures = fallbackDepartures();
        }

        String code = uniqueCode(route.shortName(), route.routeId(), usedCodes);
        String name = (route.longName() != null && !route.longName().isBlank())
                ? CtanTextNormalizer.lineName(route.longName())
                : code;
        String color = (route.color() != null && route.color().length() >= 4)
                ? route.color()
                : fallbackColor;

        Optional<Line> existing = lineRepository.findByCode(code);
        Line line;
        if (existing.isPresent()) {
            line = existing.get();
            line.getStops().clear();
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
            line.getStops().add(LineStop.builder()
                    .line(line)
                    .stop(orderedStops.get(i))
                    .sequence(i + 1)
                    .minutesFromStart(minutes.get(i))
                    .build());
        }
        line.getDepartureTimes().addAll(departures);

        lineRepository.save(line);
        return true;
    }

    // ── Paradas ─────────────────────────────────────────────────────────────

    private Stop upsertStop(GtfsStop gs) {
        String code = "CTAN-" + stripConsorcio(gs.stopId());
        Stop stop = stopRepository.findByCode(code).orElseGet(Stop::new);
        stop.setCode(code);
        stop.setName(CtanTextNormalizer.stopName(gs.name()));
        stop.setLatitude(gs.lat());
        stop.setLongitude(gs.lon());
        return stopRepository.save(stop);
    }

    /** Convierte el {@code stop_id} GTFS ("7_1") en el identificador de parada ("1"). */
    private String stripConsorcio(String stopId) {
        int idx = stopId.indexOf('_');
        return idx >= 0 ? stopId.substring(idx + 1) : stopId;
    }

    // ── Horarios ────────────────────────────────────────────────────────────

    /** Minutos acumulados desde la cabecera, estrictamente crecientes. */
    private List<Integer> minutesFromStart(List<Integer> departureSeconds) {
        List<Integer> minutes = new ArrayList<>();
        int firstValid = -1;
        for (int sec : departureSeconds) {
            if (sec >= 0) {
                firstValid = sec;
                break;
            }
        }
        int prev = -1;
        for (int sec : departureSeconds) {
            int m;
            if (firstValid < 0 || sec < 0) {
                m = prev + 1;
            } else {
                m = Math.round((sec - firstValid) / 60.0f);
            }
            if (m <= prev) {
                m = prev + 1;
            }
            minutes.add(m);
            prev = m;
        }
        if (!minutes.isEmpty()) {
            minutes.set(0, 0);
        }
        return minutes;
    }

    /** Horas de salida de cabecera (primera parada) de las expediciones del sentido elegido. */
    private List<LocalTime> collectDepartures(List<GtfsTrip> trips, String direction, GtfsDataset data) {
        Set<LocalTime> times = new TreeSet<>();
        for (GtfsTrip t : trips) {
            if (direction != null && !direction.equals(t.directionId())) {
                continue;
            }
            List<GtfsStopTime> st = data.stopTimesByTrip().get(t.tripId());
            if (st == null || st.isEmpty()) {
                continue;
            }
            int sec = st.get(0).departureSeconds();
            if (sec >= 0) {
                times.add(LocalTime.ofSecondOfDay(sec % 86400L));
            }
        }
        return new ArrayList<>(times);
    }

    private List<LocalTime> fallbackDepartures() {
        List<LocalTime> times = new ArrayList<>();
        for (int hour = 7; hour <= 21; hour += 2) {
            times.add(LocalTime.of(hour, 0));
        }
        return times;
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    private String uniqueCode(String shortName, String routeId, Set<String> usedCodes) {
        String base = (shortName != null && !shortName.isBlank()) ? shortName.trim() : routeId;
        String code = base;
        if (usedCodes.contains(code)) {
            code = base + "-" + routeId;
        }
        usedCodes.add(code);
        return code;
    }
}
