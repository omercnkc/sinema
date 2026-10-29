package com.sinema.testdestegi;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Her servlet servisinin ortak altyapı testleri. Servis test sınıfı bunu miras alır;
 * @SpringBootTest uygulama sınıfını alt sınıfın paketinden bulur, böylece her servis kendi context'iyle test edilir.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MysqlTestYapilandirmasi.class)
public abstract class ServisAltyapiTesti {

    private static final String KORELASYON_BASLIGI = "X-Correlation-Id";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReportUpWithCorrelationId_whenHealthEndpointCalled() throws Exception {
        HttpResponse<String> cevap = getIstegiGonder("/actuator/health");

        assertThat(cevap.statusCode()).isEqualTo(200);
        assertThat(cevap.body()).contains("\"status\":\"UP\"");
        assertThat(cevap.headers().firstValue(KORELASYON_BASLIGI)).isPresent();
    }

    // Korumalı serviste token'sız istek adres var mı yok mu öğrenemez (401); korumasızda 404. İkisinde de biçim aynı.
    @Test
    void shouldReturnProblemJsonWithCode_whenPathDoesNotExist() throws Exception {
        HttpResponse<String> cevap = getIstegiGonder("/olmayan-adres");

        assertThat(cevap.statusCode()).isIn(401, 404);
        assertThat(cevap.headers().firstValue("Content-Type")).hasValueSatisfying(
                tip -> assertThat(tip).startsWith("application/problem+json"));
        assertThat(cevap.body()).containsPattern("\"code\":\"(NOT_FOUND|UNAUTHORIZED)\"").contains("\"correlationId\"");
    }

    @Test
    void shouldApplyBaselineMigration_whenApplicationStarts() {
        Integer basariliSurumSayisi = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1", Integer.class);

        assertThat(basariliSurumSayisi).isEqualTo(1);
    }

    private HttpResponse<String> getIstegiGonder(String yol) throws Exception {
        HttpRequest istek = HttpRequest.newBuilder(URI.create("http://localhost:" + port + yol)).build();
        return HttpClient.newHttpClient().send(istek, HttpResponse.BodyHandlers.ofString());
    }
}
