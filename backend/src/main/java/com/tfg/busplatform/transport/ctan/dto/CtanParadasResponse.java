package com.tfg.busplatform.transport.ctan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Respuesta de GET /Consorcios/{id}/paradas (todas las paradas del consorcio). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CtanParadasResponse(List<CtanParada> paradas) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CtanParada(
            String idParada,
            String nombre,
            String latitud,
            String longitud,
            String municipio,
            String nucleo
    ) {}
}
