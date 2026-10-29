package com.sinema.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sinema.testdestegi.TestTokenUretici;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * Gerçek gateway + sahte arka uç: dört servisin adresi, gelen her isteği kaydeden tek bir küçük
 * sunucuya yönlendirilir. Böylece bir isteğin servise ULAŞIP ulaşmadığı ve hangi başlıklarla ulaştığı görülür.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "sinema.gateway.cors.izinli-originler=" + GatewayEntegrasyonTest.FRONTEND)
class GatewayEntegrasyonTest {

    static final String FRONTEND = "http://localhost:4200";

    private static final List<GelenIstek> ARKA_UCA_GELENLER = new CopyOnWriteArrayList<>();
    private static final DisposableServer SAHTE_ARKA_UC = HttpServer.create().port(0)
            .handle((istek, cevap) -> {
                ARKA_UCA_GELENLER.add(new GelenIstek(istek.uri(), istek.requestHeaders().get(HttpHeaders.AUTHORIZATION),
                        istek.requestHeaders().get("X-Correlation-Id")));
                // Gerçek servisler gibi korelasyon başlığını geri döner (gateway bunu tekrarlamamalı)
                String kimlik = istek.requestHeaders().get("X-Correlation-Id");
                return cevap.status(200).header("X-Correlation-Id", kimlik == null ? "" : kimlik)
                        .sendString(Mono.just("arka-uc"));
            })
            .bindNow();

    @Value("${local.server.port}")
    private int port;

    private WebTestClient istemci;

    @DynamicPropertySource
    static void servisAdresleri(DynamicPropertyRegistry kayit) {
        String adres = "http://localhost:" + SAHTE_ARKA_UC.port();
        for (String servis : List.of("AUTH", "CATALOG", "BOOKING", "PAYMENT")) {
            kayit.add(servis + "_SERVICE_URL", () -> adres);
        }
    }

    @BeforeEach
    void kur() {
        ARKA_UCA_GELENLER.clear();
        istemci = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @AfterAll
    static void kapat() {
        SAHTE_ARKA_UC.disposeNow();
    }

    @Test
    void shouldForwardPublicCatalogRequestWithCorrelationId_whenNoTokenGiven() {
        istemci.get().uri("/api/v1/catalog/movies").exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Correlation-Id");

        assertThat(ARKA_UCA_GELENLER).singleElement().satisfies(gelen -> {
            assertThat(gelen.yol()).isEqualTo("/api/v1/catalog/movies");
            assertThat(gelen.korelasyonKimligi()).isNotBlank();
        });
    }

    @Test
    void shouldKeepValidIncomingCorrelationIdAndReplaceUnsafeOne() {
        // valueEquals tek değer bekler: başlık iki kez gelseydi (gateway + servis) test düşerdi
        istemci.get().uri("/api/v1/catalog/a").header("X-Correlation-Id", "istemci-123").exchange()
                .expectHeader().valueEquals("X-Correlation-Id", "istemci-123");
        istemci.get().uri("/api/v1/catalog/b").header("X-Correlation-Id", "kotu deger;").exchange();

        assertThat(ARKA_UCA_GELENLER.get(0).korelasyonKimligi()).isEqualTo("istemci-123");
        assertThat(ARKA_UCA_GELENLER.get(1).korelasyonKimligi()).matches("[A-Za-z0-9-]+");
    }

    @Test
    void shouldRejectWithProblemJsonAndNotForward_whenProtectedPathHasNoToken() {
        istemci.get().uri("/api/v1/bookings/reservations").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith("application/problem+json")
                .expectBody().jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.correlationId").isNotEmpty();

        assertThat(ARKA_UCA_GELENLER).isEmpty();
    }

    @Test
    void shouldForwardAuthorizationHeaderUnchanged_whenUserTokenIsValid() {
        String token = TestTokenUretici.kullaniciTokeni(7, "USER");

        istemci.get().uri("/api/v1/bookings/reservations").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange().expectStatus().isOk();

        assertThat(ARKA_UCA_GELENLER).singleElement()
                .satisfies(gelen -> assertThat(gelen.yetkiBasligi()).isEqualTo("Bearer " + token));
    }

    @Test
    void shouldEnforceRouteRoles_beforeReachingServices() {
        assertThat(durum("/api/v1/admin/catalog/movies", TestTokenUretici.kullaniciTokeni(7, "USER"))).isEqualTo(403);
        assertThat(durum("/api/v1/admin/catalog/movies", TestTokenUretici.kullaniciTokeni(1, "ADMIN"))).isEqualTo(200);
        assertThat(durum("/api/v1/bookings/x", TestTokenUretici.servisTokeni("payment-service", "booking-service")))
                .as("servis token'ı dışarıdan kullanılamaz").isEqualTo(403);
        assertThat(durum("/api/v1/bookings/x", TestTokenUretici.suresiDolmusKullaniciTokeni())).isEqualTo(401);
        assertThat(durum("/api/v1/bookings/x", TestTokenUretici.yabanciAnahtarlaImzaliToken())).isEqualTo(401);

        assertThat(ARKA_UCA_GELENLER).as("yalnızca admin isteği servise ulaşmalı").hasSize(1);
    }

    @Test
    void shouldNeverExposeInternalOrActuatorMetrics() {
        assertThat(durum("/internal/v1/auth/service-token", null)).isIn(401, 403);
        assertThat(durum("/internal/v1/x", TestTokenUretici.servisTokeni("payment-service", "booking-service")))
                .isIn(401, 403);
        assertThat(durum("/actuator/metrics", TestTokenUretici.kullaniciTokeni(1, "ADMIN"))).isEqualTo(403);
        assertThat(ARKA_UCA_GELENLER).isEmpty();
    }

    @Test
    void shouldAllowCorsPreflight_onlyFromConfiguredFrontend() {
        istemci.options().uri("/api/v1/bookings/reservations")
                .header(HttpHeaders.ORIGIN, FRONTEND)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

        istemci.options().uri("/api/v1/bookings/reservations")
                .header(HttpHeaders.ORIGIN, "https://kotu-site.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldRequireAllowedOrigin_forCookieBasedRefreshAndLogout() {
        istemci.post().uri("/api/v1/auth/refresh").exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.code").isEqualTo("ORIGIN_NOT_ALLOWED");
        istemci.post().uri("/api/v1/auth/logout").header(HttpHeaders.ORIGIN, "https://kotu-site.example").exchange()
                .expectStatus().isForbidden();
        assertThat(ARKA_UCA_GELENLER).isEmpty();

        istemci.post().uri("/api/v1/auth/refresh").header(HttpHeaders.ORIGIN, FRONTEND).exchange()
                .expectStatus().isOk();
        assertThat(ARKA_UCA_GELENLER).hasSize(1);
    }

    @Test
    void shouldForwardIyzicoCallback_evenWithForeignOrigin() {
        // iyzico sayfasından gelen form POST'u: Origin iyzico'dur, CORS bu yola uygulanmaz
        istemci.post().uri("/api/v1/payments/callback/iyzico")
                .header(HttpHeaders.ORIGIN, "https://sandbox-api.iyzipay.com")
                .exchange()
                .expectStatus().isOk();
        assertThat(ARKA_UCA_GELENLER).hasSize(1);
    }

    private int durum(String yol, String token) {
        WebTestClient.RequestHeadersSpec<?> istek = istemci.get().uri(yol);
        if (token != null) {
            istek = istek.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return istek.exchange().returnResult(String.class).getStatus().value();
    }

    record GelenIstek(String yol, String yetkiBasligi, String korelasyonKimligi) {
    }
}
