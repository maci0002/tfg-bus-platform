package com.tfg.busplatform.transport.gtfs;

import com.tfg.busplatform.transport.gtfs.GtfsDataset.GtfsRoute;
import com.tfg.busplatform.transport.gtfs.GtfsDataset.GtfsStop;
import com.tfg.busplatform.transport.gtfs.GtfsDataset.GtfsStopTime;
import com.tfg.busplatform.transport.gtfs.GtfsDataset.GtfsTrip;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Parser en streaming de un feed GTFS (zip) que extrae únicamente los datos de
 * una agencia ({@code agency_id}) para minimizar la memoria utilizada: el
 * feed unificado de CTAN contiene los 9 consorcios y ficheros de hasta ~14 MB.
 *
 * <p>Orden de lectura para poder filtrar en cascada:
 * {@code routes → trips → stop_times → stops}.</p>
 */
@Component
@Slf4j
public class GtfsParser {

    /**
     * Lee el zip GTFS de {@code path} y devuelve el subconjunto de la agencia
     * indicada ya estructurado en memoria.
     */
    public GtfsDataset parse(Path path, String agencyId) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            Map<String, GtfsRoute> routes = parseRoutes(zip, agencyId);
            Map<String, GtfsTrip> trips = parseTrips(zip, routes.keySet());
            Map<String, List<GtfsStopTime>> stopTimes = parseStopTimes(zip, trips.keySet());
            Set<String> referencedStops = new HashSet<>();
            stopTimes.values().forEach(list -> list.forEach(st -> referencedStops.add(st.stopId())));
            Map<String, GtfsStop> stops = parseStops(zip, referencedStops);

            log.info("GTFS: agencia {} -> {} rutas, {} expediciones, {} paradas.",
                    agencyId, routes.size(), trips.size(), stops.size());
            return new GtfsDataset(routes, trips, stopTimes, stops);
        }
    }

    // ── routes.txt ────────────────────────────────────────────────────────────

    private Map<String, GtfsRoute> parseRoutes(ZipFile zip, String agencyId) throws IOException {
        Map<String, GtfsRoute> routes = new HashMap<>();
        readCsv(zip, "routes.txt", (cols, row) -> {
            if (!agencyId.equals(get(row, cols, "agency_id"))) {
                return;
            }
            String routeId = get(row, cols, "route_id");
            routes.put(routeId, new GtfsRoute(
                    routeId,
                    get(row, cols, "route_short_name"),
                    get(row, cols, "route_long_name"),
                    normalizeColor(get(row, cols, "route_color"))));
        });
        return routes;
    }

    // ── trips.txt ─────────────────────────────────────────────────────────────

    private Map<String, GtfsTrip> parseTrips(ZipFile zip, Set<String> routeIds) throws IOException {
        Map<String, GtfsTrip> trips = new HashMap<>();
        readCsv(zip, "trips.txt", (cols, row) -> {
            String routeId = get(row, cols, "route_id");
            if (!routeIds.contains(routeId)) {
                return;
            }
            String tripId = get(row, cols, "trip_id");
            trips.put(tripId, new GtfsTrip(tripId, routeId, get(row, cols, "direction_id")));
        });
        return trips;
    }

    // ── stop_times.txt (fichero grande) ─────────────────────────────────────────

    private Map<String, List<GtfsStopTime>> parseStopTimes(ZipFile zip, Set<String> tripIds)
            throws IOException {
        Map<String, List<GtfsStopTime>> byTrip = new HashMap<>();
        readCsv(zip, "stop_times.txt", (cols, row) -> {
            String tripId = get(row, cols, "trip_id");
            if (!tripIds.contains(tripId)) {
                return;
            }
            int seq = parseInt(get(row, cols, "stop_sequence"), 0);
            int dep = parseTimeToSeconds(get(row, cols, "departure_time"));
            byTrip.computeIfAbsent(tripId, k -> new ArrayList<>())
                    .add(new GtfsStopTime(seq, get(row, cols, "stop_id"), dep));
        });
        byTrip.values().forEach(list -> list.sort((a, b) -> Integer.compare(a.sequence(), b.sequence())));
        return byTrip;
    }

    // ── stops.txt ──────────────────────────────────────────────────────────────

    private Map<String, GtfsStop> parseStops(ZipFile zip, Set<String> stopIds) throws IOException {
        Map<String, GtfsStop> stops = new HashMap<>();
        readCsv(zip, "stops.txt", (cols, row) -> {
            String stopId = get(row, cols, "stop_id");
            if (!stopIds.contains(stopId)) {
                return;
            }
            Double lat = parseDouble(get(row, cols, "stop_lat"));
            Double lon = parseDouble(get(row, cols, "stop_lon"));
            String name = get(row, cols, "stop_name");
            if (lat == null || lon == null || name == null || name.isBlank()) {
                return;
            }
            stops.put(stopId, new GtfsStop(stopId, name, lat, lon));
        });
        return stops;
    }

    // ── Lectura CSV genérica ────────────────────────────────────────────────────

    @FunctionalInterface
    private interface RowConsumer {
        void accept(Map<String, Integer> columns, String[] row);
    }

    private void readCsv(ZipFile zip, String entryName, RowConsumer consumer) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            throw new IOException("El feed GTFS no contiene " + entryName);
        }
        try (InputStream in = zip.getInputStream(entry);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                return;
            }
            Map<String, Integer> columns = headerIndex(header);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                consumer.accept(columns, parseCsvLine(line));
            }
        }
    }

    private Map<String, Integer> headerIndex(String header) {
        String[] cols = parseCsvLine(stripBom(header));
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < cols.length; i++) {
            index.put(cols[i].trim(), i);
        }
        return index;
    }

    /** Divide una línea CSV respetando comillas dobles y comillas escapadas (""). */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    private String get(String[] row, Map<String, Integer> cols, String name) {
        Integer idx = cols.get(name);
        if (idx == null || idx >= row.length) {
            return null;
        }
        String v = row[idx];
        return v == null ? null : v.trim();
    }

    private String stripBom(String s) {
        return (s != null && !s.isEmpty() && s.charAt(0) == '﻿') ? s.substring(1) : s;
    }

    private String normalizeColor(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        String h = hex.trim();
        return h.startsWith("#") ? h : "#" + h;
    }

    private int parseInt(String v, int def) {
        try {
            return v == null || v.isBlank() ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private Double parseDouble(String v) {
        try {
            return v == null || v.isBlank() ? null : Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Convierte "HH:MM:SS" (puede superar 24h) a segundos desde medianoche. */
    private int parseTimeToSeconds(String v) {
        if (v == null || v.isBlank()) {
            return -1;
        }
        String[] parts = v.trim().split(":");
        if (parts.length < 2) {
            return -1;
        }
        try {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int s = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return h * 3600 + m * 60 + s;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
