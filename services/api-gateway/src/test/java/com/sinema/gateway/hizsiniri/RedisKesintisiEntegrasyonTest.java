package com.sinema.gateway.hizsiniri;

import static org.assertj.core.api.Assertions.assertThat;

import com.sinema.gateway.SahteArkaUc;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * K38: Redis'e ulaşılamazken genel trafik geçer (fail-open), giriş/kayıt reddedilir (fail-closed).
 * Redis adresi bilerek boş bir porta yönlendirilir: bağlantı her seferinde reddedilir.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedisKesintisiEntegrasyonTest {

    private static final SahteArkaUc ARKA_UC = new SahteArkaUc();

    @Value("${local.server.port}")
    private int port;

    private WebTestClient istemci;

    @DynamicPropertySource
    static void ayarlar(DynamicPropertyRegistry kayit) throws IOException {
        int bosPort;
        try (ServerSocket soket = new ServerSocket(0)) {
            bosPort = soket.getLocalPort(); // kapatılınca orada dinleyen kimse kalmaz
        }
        int redisPortu = bosPort;
        kayit.add("spring.data.redis.host", () -> "localhost");
        kayit.add("spring.data.redis.port", () -> redisPortu);
        ARKA_UC.adresleriKaydet(kayit);
    }

    @BeforeEach
    void kur() {
        ARKA_UC.temizle();
        istemci = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @AfterAll
    static void kapat() {
        ARKA_UC.kapat();
    }

    @Test
    void shouldRejectLoginAndRegister_whenRedisIsUnavailable() {
        istemci.post().uri("/api/v1/auth/login").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.code").isEqualTo(HizSiniriFiltresi.SINIR_KULLANILAMIYOR_KODU);
        istemci.post().uri("/api/v1/auth/register").exchange().expectStatus().isEqualTo(503);

        assertThat(ARKA_UC.gelenSayisi()).isZero();
    }

    @Test
    void shouldLetGeneralTrafficThrough_whenRedisIsUnavailable() {
        istemci.get().uri("/api/v1/catalog/movies").exchange().expectStatus().isOk();
        istemci.get().uri("/api/v1/tickets/ABC123").exchange().expectStatus().isOk();

        assertThat(ARKA_UC.gelenSayisi()).isEqualTo(2);
    }
}
