package com.tfg.busplatform.transport.gtfs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Descarga el feed GTFS (zip) a un fichero temporal del sistema.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GtfsDownloader {

    private final GtfsProperties props;

    /**
     * Descarga el feed a un fichero temporal y devuelve su ruta.
     *
     * @throws IOException          si falla la descarga o el guardado
     * @throws InterruptedException si se interrumpe la petición
     */
    public Path download() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getUrl()))
                .timeout(Duration.ofMillis(props.getReadTimeoutMs()))
                .GET()
                .build();

        Path target = Files.createTempFile("ctan-gtfs-", ".zip");
        log.info("GTFS: descargando feed desde {}", props.getUrl());
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));

        if (response.statusCode() != 200) {
            Files.deleteIfExists(target);
            throw new IOException("Respuesta HTTP " + response.statusCode() + " al descargar el GTFS");
        }
        log.info("GTFS: feed descargado ({} bytes) en {}", Files.size(target), target);
        return target;
    }
}
