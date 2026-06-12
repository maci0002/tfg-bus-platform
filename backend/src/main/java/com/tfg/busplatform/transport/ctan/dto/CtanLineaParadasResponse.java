package com.tfg.busplatform.transport.ctan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Respuesta de GET /Consorcios/{id}/lineas/{idLinea}/paradas (paradas ordenadas de una línea). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CtanLineaParadasResponse(List<CtanLineaParada> paradas) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CtanLineaParada(
            String idParada,
            String nombre,
            String latitud,
            String longitud,
            String sentido,
            Integer orden
    ) {}
}
