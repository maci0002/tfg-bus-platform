package com.tfg.busplatform.transport.ctan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Respuesta de GET /Consorcios/{id}/horarios_lineas?linea={idLinea}&sentido={1|2}
 *
 * <p>Cada planificador agrupa los servicios (expediciones) de un periodo. Para cada
 * servicio, {@code horas} contiene las horas de paso por los bloques horarios; la
 * primera hora corresponde a la salida desde la cabecera.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CtanHorariosResponse(List<CtanPlanificador> planificadores) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CtanPlanificador(
            List<CtanServicio> horarioIda,
            List<CtanServicio> horarioVuelta
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CtanServicio(
            List<String> horas,
            String nombre
    ) {}
}
