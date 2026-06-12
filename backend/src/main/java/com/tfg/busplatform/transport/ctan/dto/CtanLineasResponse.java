package com.tfg.busplatform.transport.ctan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Respuesta de GET /Consorcios/{id}/lineas */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CtanLineasResponse(List<CtanLinea> lineas) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CtanLinea(
            String idLinea,
            String codigo,
            String nombre,
            String modo,
            String operadores,
            String hayNoticia
    ) {}
}
