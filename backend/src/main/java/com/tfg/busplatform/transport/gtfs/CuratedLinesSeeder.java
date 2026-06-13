package com.tfg.busplatform.transport.gtfs;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Siembra un conjunto de líneas interurbanas <strong>rurales</strong> de la
 * provincia de Jaén que no forman parte del consorcio metropolitano (consorcio
 * 7 de CTAN) y que, por tanto, no están disponibles en la API ni en el GTFS.
 *
 * <p>Son datos curados manualmente para enriquecer la cobertura provincial del
 * prototipo: las coordenadas de los municipios son reales, mientras que los
 * horarios son representativos. Se marcan con el prefijo de código {@code JA-}
 * y se cargan de forma idempotente como complemento de los datos oficiales.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CuratedLinesSeeder {

    private final StopRepository stopRepository;
    private final LineRepository lineRepository;

    /** Velocidad media (km/h) para estimar tiempos en estas líneas interurbanas. */
    private static final double AVG_SPEED_KMH = 55.0;

    /** Horario representativo de salidas de cabecera. */
    private static final List<LocalTime> DEPARTURES = List.of(
            LocalTime.of(7, 0), LocalTime.of(9, 30), LocalTime.of(12, 0),
            LocalTime.of(14, 30), LocalTime.of(17, 0), LocalTime.of(19, 30));

    /** Registro de municipios/núcleos con sus coordenadas reales. */
    private static final Map<String, Town> TOWNS = new LinkedHashMap<>();

    private record Town(String code, String name, double lat, double lng) {}

    static {
        town("JAEN", "Jaén (Estación de Autobuses)", 37.779600, -3.784900);
        town("MANCHAREAL", "Mancha Real", 37.788900, -3.612500);
        town("BAEZA", "Baeza", 37.992500, -3.469900);
        town("UBEDA", "Úbeda", 38.011700, -3.370600);
        town("LINARES", "Linares", 38.095600, -3.636600);
        town("BAILEN", "Bailén", 38.096700, -3.776900);
        town("ANDUJAR", "Andújar", 38.039400, -4.050600);
        town("MARTOS", "Martos", 37.721300, -3.970600);
        town("TORREDELCAMPO", "Torredelcampo", 37.766500, -3.886900);
        town("TORREDONJIMENO", "Torredonjimeno", 37.765700, -3.951000);
        town("ALCAUDETE", "Alcaudete", 37.590700, -4.086700);
        town("CAZORLA", "Cazorla", 37.910500, -3.002800);
        town("QUESADA", "Quesada", 37.844700, -3.068600);
        town("JODAR", "Jódar", 37.846000, -3.351000);
        town("HUELMA", "Huelma", 37.648600, -3.456100);
        town("PEGALAJAR", "Pegalajar", 37.743000, -3.645300);
        town("VILLACARRILLO", "Villacarrillo", 38.117000, -3.084300);
        town("SANTISTEBAN", "Santisteban del Puerto", 38.248300, -3.207800);
        town("LACAROLINA", "La Carolina", 38.275600, -3.616800);
    }

    private static void town(String code, String name, double lat, double lng) {
        TOWNS.put(code, new Town(code, name, lat, lng));
    }

    /** Definición de una línea curada: código, nombre, color y núcleos del recorrido. */
    private record RuralLine(String code, String name, String color, List<String> towns) {}

    private static final List<RuralLine> LINES = List.of(
            new RuralLine("JA-01", "Jaén - Mancha Real - Baeza - Úbeda", "#00695C",
                    List.of("JAEN", "MANCHAREAL", "BAEZA", "UBEDA")),
            new RuralLine("JA-02", "Úbeda - Baeza - Linares", "#283593",
                    List.of("UBEDA", "BAEZA", "LINARES")),
            new RuralLine("JA-03", "Jaén - Torredelcampo - Torredonjimeno - Martos", "#AD1457",
                    List.of("JAEN", "TORREDELCAMPO", "TORREDONJIMENO", "MARTOS")),
            new RuralLine("JA-04", "Linares - Bailén - Andújar", "#EF6C00",
                    List.of("LINARES", "BAILEN", "ANDUJAR")),
            new RuralLine("JA-05", "Úbeda - Jódar - Quesada - Cazorla", "#4527A0",
                    List.of("UBEDA", "JODAR", "QUESADA", "CAZORLA")),
            new RuralLine("JA-06", "Jaén - Martos - Alcaudete", "#2E7D32",
                    List.of("JAEN", "MARTOS", "ALCAUDETE")),
            new RuralLine("JA-07", "Úbeda - Villacarrillo - Santisteban - La Carolina", "#00838F",
                    List.of("UBEDA", "VILLACARRILLO", "SANTISTEBAN", "LACAROLINA")),
            new RuralLine("JA-08", "Jaén - Pegalajar - Huelma - Quesada", "#6A1B9A",
                    List.of("JAEN", "PEGALAJAR", "HUELMA", "QUESADA")));

    /**
     * Carga (idempotente) las líneas rurales curadas. Las líneas ya existentes
     * por código se omiten.
     *
     * @return número de líneas nuevas creadas
     */
    @Transactional
    public int seed() {
        int created = 0;
        for (RuralLine def : LINES) {
            if (lineRepository.findByCode(def.code()).isPresent()) {
                continue;
            }
            createLine(def);
            created++;
        }
        if (created > 0) {
            log.info("Líneas rurales curadas añadidas: {}.", created);
        }
        return created;
    }

    private void createLine(RuralLine def) {
        List<Stop> stops = new ArrayList<>();
        for (String townCode : def.towns()) {
            stops.add(upsertTownStop(TOWNS.get(townCode)));
        }

        Line line = Line.builder()
                .code(def.code())
                .name(def.name())
                .color(def.color())
                .stops(new ArrayList<>())
                .departureTimes(new ArrayList<>())
                .build();

        double cumulativeKm = 0;
        int prevMinutes = 0;
        for (int i = 0; i < stops.size(); i++) {
            int minutes;
            if (i == 0) {
                minutes = 0;
            } else {
                cumulativeKm += haversineKm(stops.get(i - 1), stops.get(i));
                minutes = (int) Math.round(cumulativeKm / AVG_SPEED_KMH * 60.0);
                if (minutes <= prevMinutes) {
                    minutes = prevMinutes + 1;
                }
            }
            prevMinutes = minutes;
            line.getStops().add(LineStop.builder()
                    .line(line)
                    .stop(stops.get(i))
                    .sequence(i + 1)
                    .minutesFromStart(minutes)
                    .build());
        }
        line.getDepartureTimes().addAll(DEPARTURES);
        lineRepository.save(line);
    }

    private Stop upsertTownStop(Town town) {
        String code = "JA-" + town.code();
        Stop stop = stopRepository.findByCode(code).orElseGet(Stop::new);
        stop.setCode(code);
        stop.setName(town.name());
        stop.setLatitude(town.lat());
        stop.setLongitude(town.lng());
        return stopRepository.save(stop);
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
}
