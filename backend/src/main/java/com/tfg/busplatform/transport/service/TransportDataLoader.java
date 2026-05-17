package com.tfg.busplatform.transport.service;

import com.tfg.busplatform.transport.model.Line;
import com.tfg.busplatform.transport.model.LineStop;
import com.tfg.busplatform.transport.model.Stop;
import com.tfg.busplatform.transport.repository.LineRepository;
import com.tfg.busplatform.transport.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga inicial de datos de transporte para el prototipo.
 * Datos aproximados del entorno de Jaén (interurbano).
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class TransportDataLoader {

    @Bean
    ApplicationRunner seedTransportData(StopRepository stopRepository, LineRepository lineRepository) {
        return args -> {
            if (lineRepository.count() > 0) {
                log.info("Datos de transporte ya cargados, omitiendo seed.");
                return;
            }

            log.info("Cargando datos de transporte iniciales (entorno de Jaén)...");

            // ── Paradas (ciudades del área de Jaén) ─────────────────────────
            Map<String, Stop> stops = new HashMap<>();
            stops.put("JAEN",   save(stopRepository, "JAEN",   "Jaén - Estación de Autobuses", 37.78088, -3.78611));
            stops.put("MR",     save(stopRepository, "MR",     "Mancha Real",                  37.78580, -3.61780));
            stops.put("BAE",    save(stopRepository, "BAE",    "Baeza",                        37.99420, -3.47080));
            stops.put("UBE",    save(stopRepository, "UBE",    "Úbeda",                        38.01360, -3.37060));
            stops.put("TORP",   save(stopRepository, "TORP",   "Torreperogil",                 38.03140, -3.29420));
            stops.put("MEN",    save(stopRepository, "MEN",    "Mengíbar",                     37.96500, -3.80810));
            stops.put("ESP",    save(stopRepository, "ESP",    "Espelúy",                      38.01500, -3.85060));
            stops.put("LIN",    save(stopRepository, "LIN",    "Linares",                      38.09720, -3.63640));
            stops.put("VR",     save(stopRepository, "VR",     "Villanueva de la Reina",       37.98170, -3.90560));
            stops.put("AND",    save(stopRepository, "AND",    "Andújar",                      38.03940, -4.05110));
            stops.put("TDC",    save(stopRepository, "TDC",    "Torredelcampo",                37.77060, -3.88690));
            stops.put("TDJ",    save(stopRepository, "TDJ",    "Torredonjimeno",               37.76470, -3.94890));
            stops.put("MAR",    save(stopRepository, "MAR",    "Martos",                       37.71970, -3.97190));
            stops.put("ALC",    save(stopRepository, "ALC",    "Alcaudete",                    37.59140, -4.08820));
            stops.put("CAZ",    save(stopRepository, "CAZ",    "Cazorla",                      37.91030, -3.00270));
            stops.put("PEAL",   save(stopRepository, "PEAL",   "Peal de Becerro",              37.91900, -3.12200));

            // ── Líneas ──────────────────────────────────────────────────────

            // L01 - Jaén → Úbeda (vía Mancha Real y Baeza)
            createLine(lineRepository, "L01", "Jaén - Úbeda", "#1976D2",
                    List.of(
                            stopAt(stops.get("JAEN"), 1, 0),
                            stopAt(stops.get("MR"),   2, 20),
                            stopAt(stops.get("BAE"),  3, 45),
                            stopAt(stops.get("UBE"),  4, 60)
                    ),
                    List.of(LocalTime.of(7, 0), LocalTime.of(9, 30), LocalTime.of(12, 0),
                            LocalTime.of(15, 0), LocalTime.of(18, 0), LocalTime.of(20, 30)));

            // L02 - Jaén → Linares (vía Mengíbar y Espelúy)
            createLine(lineRepository, "L02", "Jaén - Linares", "#D32F2F",
                    List.of(
                            stopAt(stops.get("JAEN"), 1, 0),
                            stopAt(stops.get("MEN"),  2, 25),
                            stopAt(stops.get("ESP"),  3, 35),
                            stopAt(stops.get("LIN"),  4, 50)
                    ),
                    List.of(LocalTime.of(7, 30), LocalTime.of(10, 0), LocalTime.of(13, 0),
                            LocalTime.of(16, 30), LocalTime.of(19, 30)));

            // L03 - Jaén → Baeza (directa)
            createLine(lineRepository, "L03", "Jaén - Baeza", "#388E3C",
                    List.of(
                            stopAt(stops.get("JAEN"), 1, 0),
                            stopAt(stops.get("MR"),   2, 20),
                            stopAt(stops.get("BAE"),  3, 50)
                    ),
                    List.of(LocalTime.of(8, 0), LocalTime.of(11, 0), LocalTime.of(14, 30),
                            LocalTime.of(17, 0), LocalTime.of(19, 0)));

            // L04 - Jaén → Andújar (vía Mengíbar y Villanueva)
            createLine(lineRepository, "L04", "Jaén - Andújar", "#F57C00",
                    List.of(
                            stopAt(stops.get("JAEN"), 1, 0),
                            stopAt(stops.get("MEN"),  2, 25),
                            stopAt(stops.get("VR"),   3, 45),
                            stopAt(stops.get("AND"),  4, 60)
                    ),
                    List.of(LocalTime.of(7, 15), LocalTime.of(10, 30), LocalTime.of(14, 0),
                            LocalTime.of(17, 30), LocalTime.of(20, 0)));

            // L05 - Jaén → Martos (vía Torredelcampo y Torredonjimeno)
            createLine(lineRepository, "L05", "Jaén - Martos", "#7B1FA2",
                    List.of(
                            stopAt(stops.get("JAEN"), 1, 0),
                            stopAt(stops.get("TDC"),  2, 10),
                            stopAt(stops.get("TDJ"),  3, 20),
                            stopAt(stops.get("MAR"),  4, 30)
                    ),
                    List.of(LocalTime.of(7, 0), LocalTime.of(8, 0), LocalTime.of(9, 0),
                            LocalTime.of(12, 0), LocalTime.of(14, 0), LocalTime.of(16, 0),
                            LocalTime.of(18, 0), LocalTime.of(20, 0)));

            // L06 - Jaén → Alcaudete (vía Martos)
            createLine(lineRepository, "L06", "Jaén - Alcaudete", "#00897B",
                    List.of(
                            stopAt(stops.get("JAEN"), 1, 0),
                            stopAt(stops.get("TDC"),  2, 10),
                            stopAt(stops.get("MAR"),  3, 30),
                            stopAt(stops.get("ALC"),  4, 55)
                    ),
                    List.of(LocalTime.of(8, 30), LocalTime.of(13, 30), LocalTime.of(18, 30)));

            // L07 - Jaén → Cazorla (vía Baeza y Peal)
            createLine(lineRepository, "L07", "Jaén - Cazorla", "#5D4037",
                    List.of(
                            stopAt(stops.get("JAEN"), 1, 0),
                            stopAt(stops.get("MR"),   2, 20),
                            stopAt(stops.get("BAE"),  3, 50),
                            stopAt(stops.get("PEAL"), 4, 85),
                            stopAt(stops.get("CAZ"),  5, 105)
                    ),
                    List.of(LocalTime.of(9, 0), LocalTime.of(14, 30), LocalTime.of(18, 0)));

            log.info("Seed completado: {} paradas y {} líneas creadas.",
                    stopRepository.count(), lineRepository.count());
        };
    }

    private Stop save(StopRepository repo, String code, String name, double lat, double lng) {
        return repo.save(Stop.builder()
                .code(code)
                .name(name)
                .latitude(lat)
                .longitude(lng)
                .build());
    }

    private LineStop stopAt(Stop stop, int sequence, int minutesFromStart) {
        return LineStop.builder()
                .stop(stop)
                .sequence(sequence)
                .minutesFromStart(minutesFromStart)
                .build();
    }

    private void createLine(LineRepository repo, String code, String name, String color,
                            List<LineStop> stops, List<LocalTime> departures) {
        Line line = Line.builder()
                .code(code)
                .name(name)
                .color(color)
                .stops(new ArrayList<>())
                .departureTimes(new ArrayList<>(departures))
                .build();

        // Asignar la línea a cada lineStop (relación bidireccional)
        for (LineStop ls : stops) {
            ls.setLine(line);
            line.getStops().add(ls);
        }

        repo.save(line);
    }
}
