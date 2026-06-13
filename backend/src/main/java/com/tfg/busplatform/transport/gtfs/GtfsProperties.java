package com.tfg.busplatform.transport.gtfs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de la ingesta del feed GTFS de la Red de Consorcios de
 * Transporte de Andalucía (CTAN).
 *
 * <p>El feed unificado contiene los 9 consorcios; mediante {@link #agencyId}
 * se filtra el de Jaén (CTJA). A diferencia de la API REST, el GTFS aporta
 * horarios reales ({@code stop_times}) y colores oficiales de línea.</p>
 */
@Component
@ConfigurationProperties(prefix = "gtfs")
@Data
public class GtfsProperties {

    /** Activa/desactiva la ingesta del feed GTFS. */
    private boolean enabled = false;

    /** URL del feed GTFS (zip). */
    private String url = "https://api.ctan.es/v1/datos/UNIFICADO/gtfs.zip";

    /** Identificador de agencia GTFS a importar (CTJA = Área de Jaén). */
    private String agencyId = "CTJA";

    /** Tiempo máximo de espera de conexión, en milisegundos. */
    private int connectTimeoutMs = 10000;

    /** Tiempo máximo de espera de lectura/descarga, en milisegundos. */
    private int readTimeoutMs = 60000;

    /**
     * Número mínimo de paradas que debe tener el recorrido de una línea para
     * cargarla. Descarta líneas degeneradas con muy pocas paradas de núcleo.
     */
    private int minStops = 3;
}
