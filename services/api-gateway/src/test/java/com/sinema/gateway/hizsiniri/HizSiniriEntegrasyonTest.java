package com.sinema.gateway.hizsiniri;

import static org.assertj.core.api.Assertions.assertThat;

import com.sinema.gateway.SahteArkaUc;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;

/**
 * Gerçek Redis (Testcontainers) ile istek sınırı. Kurallar testte küçültülür ve 1 dakikalık tutulur:
 * test süresince jeton yenilenmez, sınır kesin olarak aşılır.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "sinema.gateway.hiz-siniri.kurallar[0].ad=giris",
        "sinema.gateway.hiz-siniri.kurallar[0].yol=POST /api/v1/auth/login",
        "sinema.gateway.hiz-siniri.kurallar[0].istek=3",
        "sinema.gateway.hiz-siniri.kurallar[0].sure=1m",
        "sinema.gateway.hiz-siniri.kurallar[0].kati=true",
        "sinema.gateway.hiz-siniri.kurallar[1].ad=genel",
        "sinema.gateway.hiz-siniri.kurallar[1].yol=/api/**",
        "sinema.gateway.hiz-siniri.kurallar[1].istek=5",
        "sinema.gateway.hiz-siniri.kurallar[1].sure=1m"})
class HizSiniriEntegrasyonTest {

    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.8.4-alpine").withExposedPorts(6379);
    private static final SahteArkaUc ARKA_UC = new SahteArkaUc();

    static {
        REDIS.start();
    }

    @Value("${local.server.port}")
    private int port;

    private WebTestClient istemci;

    @DynamicPropertySource
    static void ayarlar(DynamicPropertyRegistry kayit) {
        kayit.add("spring.data.redis.host", REDIS::getHost);
        kayit.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        ARKA_UC.adresleriKaydet(kayit);
    }

    @BeforeEach
    void kur() throws Exception {
        REDIS.execInContainer("redis-cli", "FLUSHALL"); // her test sıfır sayaçla başlar
        ARKA_UC.temizle();
        istemci = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @AfterAll
    static void kapat() {
        ARKA_UC.kapat();
        REDIS.stop();
    }

    @Test
    void shouldRejectWithProblemJsonAndRetryAfter_whenLoginLimitExceeded() {
        for (int i = 0; i < 3; i++) {
            girisDene().expectStatus().isOk();
        }

        girisDene()
                .expectStatus().isEqualTo(429)
                .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "20") // 60 sn / 3 istek
                .expectHeader().contentTypeCompatibleWith("application/problem+json")
                .expectBody().jsonPath("$.code").isEqualTo(HizSiniriFiltresi.SINIR_ASILDI_KODU);

        assertThat(ARKA_UC.gelenSayisi()).as("sınırı aşan istek servise ulaşmamalı").isEqualTo(3);
    }

    @Test
    void shouldKeepSeparateCounters_whenLoginLimitExhausted() {
        for (int i = 0; i < 4; i++) {
            girisDene();
        }

        // Giriş sayacı doldu ama genel sayaç ayrı: katalog gezmek etkilenmez
        istemci.get().uri("/api/v1/catalog/movies").exchange().expectStatus().isOk();
    }

    @Test
    void shouldApplyGeneralLimit_toOtherApiPaths() {
        for (int i = 0; i < 5; i++) {
            istemci.get().uri("/api/v1/catalog/movies").exchange().expectStatus().isOk();
        }

        istemci.get().uri("/api/v1/catalog/movies").exchange().expectStatus().isEqualTo(429);
    }

    private WebTestClient.ResponseSpec girisDene() {
        return istemci.post().uri("/api/v1/auth/login").exchange();
    }
}
