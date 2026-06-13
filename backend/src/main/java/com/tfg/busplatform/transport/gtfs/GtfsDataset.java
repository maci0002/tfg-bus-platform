package com.tfg.busplatform.transport.gtfs;

import java.util.List;
import java.util.Map;

/**
 * Subconjunto del feed GTFS ya filtrado para una agencia (Jaén/CTJA) y
 * estructurado en memoria para el mapeo a entidades de dominio.
 *
 * @param routes          rutas indexadas por {@code route_id}
 * @param trips           viajes indexados por {@code trip_id}
 * @param stopTimesByTrip paradas-horario de cada viaje, ordenadas por secuencia
 * @param stops           paradas indexadas por {@code stop_id}
 */
public record GtfsDataset(
        Map<String, GtfsRoute> routes,
        Map<String, GtfsTrip> trips,
        Map<String, List<GtfsStopTime>> stopTimesByTrip,
        Map<String, GtfsStop> stops
) {

    /** Línea comercial GTFS ({@code routes.txt}). */
    public record GtfsRoute(String routeId, String shortName, String longName, String color) {}

    /** Expedición GTFS ({@code trips.txt}). */
    public record GtfsTrip(String tripId, String routeId, String directionId) {}

    /** Paso por parada de una expedición ({@code stop_times.txt}). */
    public record GtfsStopTime(int sequence, String stopId, int departureSeconds) {}

    /** Parada física GTFS ({@code stops.txt}). */
    public record GtfsStop(String stopId, String name, double lat, double lon) {}
}
