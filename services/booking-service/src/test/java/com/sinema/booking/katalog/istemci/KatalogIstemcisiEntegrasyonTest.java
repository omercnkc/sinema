package com.sinema.booking.katalog.istemci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import com.sinema.common.korelasyon.KorelasyonKimligi;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import com.sinema.testdestegi.TestJwtAnahtarBaslaticisi;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * KatalogIstemcisi'ni ortak Feign altyapısıyla birlikte gerçek HTTP üzerinden dener: servis token'ı alınması,
 * başlıkların iletimi, JSON eşlemesi, hata çevirisi, zaman aşımı ve GET'e özel yeniden deneme.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MysqlTestYapilandirmasi.class)
class KatalogIstemcisiEntegrasyonTest {

    private static final String TOKEN_YOLU = "/internal/v1/auth/service-token";
    private static final String SEANS_YOLU = "/internal/v1/catalog/showtimes/7";
    private static final String COZUMLEME_YOLU = SEANS_YOLU + "/seats/resolve";
    private static final long OKUMA_ZAMAN_ASIMI_MS = 300;

    private static final SahteServisSunucusu SAHTE_SUNUCU = sunucuBaslat();

    @Autowired
    private KatalogIstemcisi katalogIstemcisi;

    @DynamicPropertySource
    static void adresler(DynamicPropertyRegistry kayit) {
        kayit.add("sinema.servis-istemcisi.auth-adresi", SAHTE_SUNUCU::adres);
        kayit.add("sinema.servisler.catalog-adresi", SAHTE_SUNUCU::adres);
        kayit.add("sinema.servis-istemcisi.okuma-zaman-asimi", () -> OKUMA_ZAMAN_ASIMI_MS + "ms");
        kayit.add("sinema.servis-istemcisi.yeniden-deneme.ilk-bekleme", () -> "1ms");
    }

    @BeforeEach
    void hazirla() {
        SAHTE_SUNUCU.sifirla();
        SAHTE_SUNUCU.cevapla(TOKEN_YOLU, 200, "{\"accessToken\":\"servis-tokeni\",\"expiresInSeconds\":300}");
    }

    @AfterEach
    void temizle() {
        MDC.clear();
    }

    @AfterAll
    static void kapat() {
        SAHTE_SUNUCU.durdur();
    }

    @Test
    void shouldSendServiceTokenAndCorrelationId_andMapShowtime_whenCatalogResponds() {
        SAHTE_SUNUCU.cevapla(SEANS_YOLU, 200, """
                {"showtimeId":7,"movieId":3,"movieTitle":"Örnek Film","cinemaId":1,"cinemaName":"Kadıköy",
                 "hallId":2,"hallName":"Salon 2","layoutId":5,"startsAt":"2026-11-01T17:00:00Z",
                 "endsAt":"2026-11-01T19:15:00Z","salesCloseAt":"2026-11-01T17:10:00Z","ticketPrice":"150.00",
                 "serverTime":"2026-10-30T10:00:00Z","yeniAlan":"catalog alan eklese de kırılmaz"}
                """);
        MDC.put(KorelasyonKimligi.MDC_ANAHTARI, "kimlik-123");

        KatalogSeansBilgisi seans = katalogIstemcisi.seansBilgisi(7L);

        assertThat(seans.movieTitle()).isEqualTo("Örnek Film");
        assertThat(seans.salesCloseAt()).isEqualTo(Instant.parse("2026-11-01T17:10:00Z"));
        assertThat(seans.ticketPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        SahteServisSunucusu.GelenIstek istek = SAHTE_SUNUCU.istekler(SEANS_YOLU).getFirst();
        assertThat(istek.yetki()).isEqualTo("Bearer servis-tokeni");
        assertThat(istek.korelasyonKimligi()).isEqualTo("kimlik-123");
    }

    @Test
    void shouldRequestTokenForCatalogAudienceWithOwnCredentials_whenNoCachedToken() {
        // Önbellekte token olabilir (testler aynı context'i paylaşır); token isteği geldiyse içeriği doğru olmalı
        SAHTE_SUNUCU.cevapla(SEANS_YOLU, 200, "{\"showtimeId\":7}");

        katalogIstemcisi.seansBilgisi(7L);

        assertThat(SAHTE_SUNUCU.istekler(TOKEN_YOLU)).allSatisfy(istek -> assertThat(istek.govde())
                .contains("\"clientId\":\"booking-service\"")
                .contains("\"audience\":\"catalog-service\"")
                .contains("\"clientSecret\":\"" + TestJwtAnahtarBaslaticisi.TEST_SERVIS_SIRRI + "\""));
    }

    @Test
    void shouldPostCellIdsAndMapResolution_whenResolvingSeats() {
        SAHTE_SUNUCU.cevapla(COZUMLEME_YOLU, 200, """
                {"layoutId":5,"seats":[{"cellId":11,"seatRowLabel":"F","seatNumber":7,"label":"F-7",
                 "seatType":"STANDARD"}],"invalidCellIds":[99]}
                """);

        KoltukCozumlemeSonucu sonuc = katalogIstemcisi.koltuklariCoz(7L, new KoltukCozumlemeIstegi(List.of(11L, 99L)));

        assertThat(sonuc.seats()).extracting(KoltukCozumlemeSonucu.Koltuk::label).containsExactly("F-7");
        assertThat(sonuc.invalidCellIds()).containsExactly(99L);
        assertThat(SAHTE_SUNUCU.istekler(COZUMLEME_YOLU).getFirst().govde()).isEqualTo("{\"cellIds\":[11,99]}");
    }

    @Test
    void shouldThrowNotFoundWithCatalogCode_whenShowtimeMissing() {
        SAHTE_SUNUCU.cevapla(SEANS_YOLU, 404, """
                {"status":404,"code":"SHOWTIME_NOT_FOUND","detail":"Seans bulunamadı."}""");

        assertThatThrownBy(() -> katalogIstemcisi.seansBilgisi(7L))
                .isInstanceOf(KaynakBulunamadiIstisnasi.class)
                .hasMessage("Seans bulunamadı.")
                .hasFieldOrPropertyWithValue("kod", "SHOWTIME_NOT_FOUND");
    }

    @Test
    void shouldThrowCatalogUnavailable_whenCatalogFailsWithServerError() {
        SAHTE_SUNUCU.cevapla(SEANS_YOLU, 500, "{\"code\":\"INTERNAL_ERROR\"}");

        assertThatThrownBy(() -> katalogIstemcisi.seansBilgisi(7L))
                .isInstanceOf(BagimlilikKullanilamazIstisnasi.class)
                .hasFieldOrPropertyWithValue("kod", KatalogIstemcisiYedegi.KOD);
        assertThat(SAHTE_SUNUCU.istekler(SEANS_YOLU)).as("5xx yeniden denenmez").hasSize(1);
    }

    @Test
    void shouldRetryGetThenThrowCatalogUnavailable_whenCatalogTimesOut() {
        SAHTE_SUNUCU.gecikmeliCevapla(SEANS_YOLU, OKUMA_ZAMAN_ASIMI_MS * 3);

        assertThatThrownBy(() -> katalogIstemcisi.seansBilgisi(7L))
                .isInstanceOf(BagimlilikKullanilamazIstisnasi.class)
                .hasFieldOrPropertyWithValue("kod", KatalogIstemcisiYedegi.KOD);
        assertThat(SAHTE_SUNUCU.istekler(SEANS_YOLU)).as("varsayılan 3 deneme").hasSize(3);
    }

    @Test
    void shouldNotRetryPost_whenCatalogTimesOut() {
        SAHTE_SUNUCU.gecikmeliCevapla(COZUMLEME_YOLU, OKUMA_ZAMAN_ASIMI_MS * 3);

        assertThatThrownBy(() -> katalogIstemcisi.koltuklariCoz(7L, new KoltukCozumlemeIstegi(List.of(11L))))
                .isInstanceOf(BagimlilikKullanilamazIstisnasi.class);
        assertThat(SAHTE_SUNUCU.istekler(COZUMLEME_YOLU)).hasSize(1);
    }

    private static SahteServisSunucusu sunucuBaslat() {
        try {
            return new SahteServisSunucusu();
        } catch (IOException e) {
            throw new IllegalStateException("Sahte sunucu başlatılamadı", e);
        }
    }
}
