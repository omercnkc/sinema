package com.sinema.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KatalogServisiUygulamasiTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void shouldReportUpWithCorrelationId_whenHealthEndpointCalled() throws Exception {
        HttpRequest istek = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).build();

        HttpResponse<String> cevap = HttpClient.newHttpClient().send(istek, HttpResponse.BodyHandlers.ofString());

        assertThat(cevap.statusCode()).isEqualTo(200);
        assertThat(cevap.body()).contains("\"status\":\"UP\"");
        assertThat(cevap.headers().firstValue("X-Correlation-Id")).isPresent();
    }
}
