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
 * Her servlet servisinin ortak altyapı ve güvenlik testleri. Servis test sınıfı bunu miras alır;
 * @SpringBootTest uygulama sınıfını alt sınıfın paketinden bulur, böylece her servis kendi context'iyle test edilir.
 *
 * Güvenlik testleri olmayan adreslere gider: amaç bir ucun işi değil, isteğin güvenlik zincirinden
 * geçip geçmediğidir. Geçerse 404 (adres yok), geçemezse 401/403 döner.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MysqlTestYapilandirmasi.class)
public abstract class ServisAltyapiTesti {

    private static final String KORELASYON_BASLIGI = "X-Correlation-Id";
    private static final String OLMAYAN_KULLANICI_YOLU = "/api/v1/olmayan-adres";
    private static final String OLMAYAN_IC_YOL = "/internal/v1/olmayan-adres";

    @Value("${local.server.port}")
    private int port;

    @Value("${spring.application.name}")
    private String servisAdi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReportUpWithCorrelationId_whenHealthEndpointCalled() throws Exception {
        HttpResponse<String> cevap = istekGonder("/actuator/health", null);

        assertThat(cevap.statusCode()).isEqualTo(200);
        assertThat(cevap.body()).contains("\"status\":\"UP\"");
        assertThat(cevap.headers().firstValue(KORELASYON_BASLIGI)).isPresent();
    }

    @Test
    void shouldApplyBaselineMigration_whenApplicationStarts() {
        Integer basariliSurumSayisi = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1", Integer.class);

        assertThat(basariliSurumSayisi).isEqualTo(1);
    }

    // ---------- kimlik doğrulama: token yok / bozuk → 401 problem+json ----------

    @Test
    void shouldReturnUnauthorizedProblemJson_whenTokenMissing() throws Exception {
        HttpResponse<String> cevap = istekGonder(OLMAYAN_KULLANICI_YOLU, null);

        assertThat(cevap.statusCode()).isEqualTo(401);
        assertThat(cevap.headers().firstValue("Content-Type")).hasValueSatisfying(
                tip -> assertThat(tip).startsWith("application/problem+json"));
        assertThat(cevap.body()).contains("\"code\":\"UNAUTHORIZED\"").contains("\"correlationId\"");
    }

    @Test
    void shouldRejectToken_whenExpiredOrWrongIssuerOrForeignKey() throws Exception {
        assertThat(istekGonder(OLMAYAN_KULLANICI_YOLU, TestTokenUretici.suresiDolmusKullaniciTokeni()).statusCode())
                .as("süresi dolmuş").isEqualTo(401);
        assertThat(istekGonder(OLMAYAN_KULLANICI_YOLU, TestTokenUretici.yanlisYayinciliKullaniciTokeni()).statusCode())
                .as("yanlış yayıncı").isEqualTo(401);
        assertThat(istekGonder(OLMAYAN_KULLANICI_YOLU, TestTokenUretici.yabanciAnahtarlaImzaliToken()).statusCode())
                .as("başka anahtarla imzalı").isEqualTo(401);
    }

    // ---------- yetki: kim nereye girebilir ----------

    @Test
    void shouldPassSecurity_whenUserTokenCallsUserPath() throws Exception {
        assertThat(istekGonder(OLMAYAN_KULLANICI_YOLU, TestTokenUretici.kullaniciTokeni(1, "USER")).statusCode())
                .isEqualTo(404);
    }

    @Test
    void shouldForbidServiceToken_whenCallingUserPath() throws Exception {
        String servisTokeni = TestTokenUretici.servisTokeni("payment-service", servisAdi);

        assertThat(istekGonder(OLMAYAN_KULLANICI_YOLU, servisTokeni).statusCode()).isEqualTo(403);
    }

    @Test
    void shouldAllowInternalPath_onlyForServiceTokenAddressedToThisService() throws Exception {
        assertThat(istekGonder(OLMAYAN_IC_YOL, TestTokenUretici.servisTokeni("payment-service", servisAdi)).statusCode())
                .as("bu servise kesilmiş servis token'ı").isEqualTo(404);
        assertThat(istekGonder(OLMAYAN_IC_YOL, TestTokenUretici.servisTokeni("payment-service", "baska-servis"))
                .statusCode()).as("başka servise kesilmiş servis token'ı").isEqualTo(403);
        assertThat(istekGonder(OLMAYAN_IC_YOL, TestTokenUretici.kullaniciTokeni(1, "ADMIN")).statusCode())
                .as("admin kullanıcı token'ı").isEqualTo(403);
    }

    @Test
    void shouldForbidRegularUser_whenCallingAdminPath() throws Exception {
        assertThat(istekGonder("/api/v1/admin/olmayan", TestTokenUretici.kullaniciTokeni(1, "USER")).statusCode())
                .isEqualTo(403);
        assertThat(istekGonder("/api/v1/admin/olmayan", TestTokenUretici.kullaniciTokeni(1, "ADMIN")).statusCode())
                .isEqualTo(404);
    }

    private HttpResponse<String> istekGonder(String yol, String token) throws Exception {
        HttpRequest.Builder istek = HttpRequest.newBuilder(URI.create("http://localhost:" + port + yol));
        if (token != null) {
            istek.header("Authorization", "Bearer " + token);
        }
        return HttpClient.newHttpClient().send(istek.build(), HttpResponse.BodyHandlers.ofString());
    }
}
