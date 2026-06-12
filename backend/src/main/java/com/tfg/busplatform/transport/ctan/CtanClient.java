package com.tfg.busplatform.transport.ctan;

import com.tfg.busplatform.transport.ctan.dto.CtanHorariosResponse;
import com.tfg.busplatform.transport.ctan.dto.CtanLineaParadasResponse;
import com.tfg.busplatform.transport.ctan.dto.CtanLineasResponse;
import com.tfg.busplatform.transport.ctan.dto.CtanParadasResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Cliente HTTP para la API abierta de la Red de Consorcios de Transporte de
 * Andalucía (CTAN). Encapsula las llamadas REST y deja el mapeo de entidades
 * al {@link CtanSyncService}.
 *
 * <p>Endpoints utilizados (consorcio {@code {c}} = idConsorcio):</p>
 * <ul>
 *   <li>GET /Consorcios/{c}/lineas</li>
 *   <li>GET /Consorcios/{c}/paradas</li>
 *   <li>GET /Consorcios/{c}/lineas/{idLinea}/paradas</li>
 *   <li>GET /Consorcios/{c}/horarios_lineas?linea={idLinea}&amp;sentido={1|2}</li>
 * </ul>
 */
@Component
@Slf4j
public class CtanClient {

    private final CtanProperties props;
    private final RestClient restClient;

    public CtanClient(CtanProperties props) {
        this.props = props;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getReadTimeoutMs());

        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    /** Lista todas las líneas del consorcio configurado. */
    public List<CtanLineasResponse.CtanLinea> getLineas() {
        CtanLineasResponse response = restClient.get()
                .uri("/Consorcios/{c}/lineas", props.getConsorcioId())
                .retrieve()
                .body(CtanLineasResponse.class);
        return response == null || response.lineas() == null ? List.of() : response.lineas();
    }

    /** Lista todas las paradas del consorcio configurado. */
    public List<CtanParadasResponse.CtanParada> getParadas() {
        CtanParadasResponse response = restClient.get()
                .uri("/Consorcios/{c}/paradas", props.getConsorcioId())
                .retrieve()
                .body(CtanParadasResponse.class);
        return response == null || response.paradas() == null ? List.of() : response.paradas();
    }

    /** Paradas ordenadas de una línea concreta (ambos sentidos). */
    public List<CtanLineaParadasResponse.CtanLineaParada> getLineaParadas(String idLinea) {
        CtanLineaParadasResponse response = restClient.get()
                .uri("/Consorcios/{c}/lineas/{l}/paradas", props.getConsorcioId(), idLinea)
                .retrieve()
                .body(CtanLineaParadasResponse.class);
        return response == null || response.paradas() == null ? List.of() : response.paradas();
    }

    /** Planificadores de horarios de una línea para un sentido (1 = ida, 2 = vuelta). */
    public CtanHorariosResponse getHorarios(String idLinea, int sentido) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/Consorcios/{c}/horarios_lineas")
                        .queryParam("linea", idLinea)
                        .queryParam("sentido", sentido)
                        .build(props.getConsorcioId()))
                .retrieve()
                .body(CtanHorariosResponse.class);
    }
}
