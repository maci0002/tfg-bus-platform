package com.tfg.busplatform.transport.ctan;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de la integración con la API abierta de la
 * Red de Consorcios de Transporte de Andalucía (CTAN).
 *
 * <p>El área de Jaén corresponde al consorcio nº 7 (CTJA).</p>
 */
@Component
@ConfigurationProperties(prefix = "ctan")
@Data
public class CtanProperties {

    /** Activa/desactiva la sincronización con la API real de CTAN. */
    private boolean enabled = false;

    /** URL base de la API (sin barra final). Ej.: http://api.ctan.es/v1 */
    private String baseUrl = "http://api.ctan.es/v1";

    /** Identificador del consorcio a consultar (7 = Área de Jaén). */
    private int consorcioId = 7;

    /** Tiempo máximo de espera de conexión, en milisegundos. */
    private int connectTimeoutMs = 5000;

    /** Tiempo máximo de espera de lectura, en milisegundos. */
    private int readTimeoutMs = 15000;

    /**
     * Velocidad media (km/h) utilizada para estimar el tiempo de viaje entre
     * paradas, ya que la API de CTAN no expone los minutos acumulados por parada.
     */
    private double averageSpeedKmh = 45.0;

    /**
     * Límite de líneas a sincronizar (0 = todas). Útil para acelerar el arranque
     * en desarrollo, donde la base de datos H2 es en memoria.
     */
    private int maxLines = 0;
}
