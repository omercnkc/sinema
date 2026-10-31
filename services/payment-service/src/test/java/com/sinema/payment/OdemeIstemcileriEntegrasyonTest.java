package com.sinema.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import com.sinema.payment.kullanici.istemci.AliciBilgisi;
import com.sinema.payment.kullanici.istemci.KullaniciIstemcisi;
import com.sinema.payment.rezervasyon.istemci.OnayIstegi;
import com.sinema.payment.rezervasyon.istemci.OnaySonucu;
import com.sinema.payment.rezervasyon.istemci.RezervasyonIstemcisi;
import com.sinema.payment.rezervasyon.istemci.RezervasyonOdemeBilgisi;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import com.sinema.testdestegi.SahteServisSunucusu;
import java.io.IOException;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * payment'ın booking ve auth istemcileri, ortak Feign altyapısıyla gerçek HTTP üzerinden: doğru hedefe kesilmiş
 * servis token'ı, JSON eşlemesi, iş sonuçlarının (404/409) kodlarıyla taşınması ve ulaşılamayan servisin 503'e çevrilmesi.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MysqlTestYapilandirmasi.class)
class OdemeIstemcileriEntegrasyonTest {

    private static final String TOKEN_YOLU = "/internal/v1/auth/service-token";
    private static final String REZERVASYON_YOLU = "/internal/v1/bookings/reservations/9";
    private static final String ALICI_YOLU = "/internal/v1/auth/users/42/billing-info";

    private static final SahteServisSunucusu SAHTE_SUNUCU = sunucuBaslat();

    @Autowired
    private RezervasyonIstemcisi rezervasyonIstemcisi;

    @Autowired
    private KullaniciIstemcisi kullaniciIstemcisi;

    @DynamicPropertySource
    static void adresler(DynamicPropertyRegistry kayit) {
        kayit.add("sinema.servis-istemcisi.auth-adresi", SAHTE_SUNUCU::adres);
        kayit.add("sinema.servisler.booking-adresi", SAHTE_SUNUCU::adres);
    }

    @BeforeEach
    void hazirla() {
        SAHTE_SUNUCU.sifirla();
        SAHTE_SUNUCU.cevapla(TOKEN_YOLU, 200, "{\"accessToken\":\"servis-tokeni\",\"expiresInSeconds\":300}");
    }

    @AfterAll
    static void kapat() {
        SAHTE_SUNUCU.durdur();
    }

    @Test
    void shouldReadServerSideAmountAndOwner_whenAskingBookingForPaymentInfo() {
        SAHTE_SUNUCU.cevapla(REZERVASYON_YOLU + "/payment-info", 200, """
                {"reservationId":9,"userId":42,"status":"PENDING_PAYMENT","totalAmount":"300.00",
                 "expiresAt":"2026-11-01T17:03:00Z","paymentWindowExtended":false,"serverTime":"2026-11-01T17:00:00Z"}
                """);

        RezervasyonOdemeBilgisi bilgi = rezervasyonIstemcisi.odemeBilgisi(9L);

        assertThat(bilgi.userId()).isEqualTo(42L);
        assertThat(bilgi.totalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(bilgi.odemeBekliyor()).isTrue();
        assertThat(SAHTE_SUNUCU.istekler(REZERVASYON_YOLU + "/payment-info").getFirst().yetki())
                .isEqualTo("Bearer servis-tokeni");
        assertThat(SAHTE_SUNUCU.istekler(TOKEN_YOLU)).allSatisfy(istek -> assertThat(istek.govde())
                .contains("\"clientId\":\"payment-service\""));
    }

    @Test
    void shouldPostPaymentIdAndReadTicketCode_whenConfirming() {
        SAHTE_SUNUCU.cevapla(REZERVASYON_YOLU + "/confirm", 200,
                "{\"reservationId\":9,\"status\":\"CONFIRMED\",\"ticketCode\":\"ABCDEFGHJK234567\"}");

        OnaySonucu sonuc = rezervasyonIstemcisi.onayla(9L, new OnayIstegi(5L));

        assertThat(sonuc.ticketCode()).isEqualTo("ABCDEFGHJK234567");
        assertThat(SAHTE_SUNUCU.istekler(REZERVASYON_YOLU + "/confirm").getFirst().govde())
                .isEqualTo("{\"paymentId\":5}");
    }

    @Test
    void shouldCarryBookingCode_whenConfirmRejectedBecauseHoldExpired() {
        SAHTE_SUNUCU.cevapla(REZERVASYON_YOLU + "/confirm", 409,
                "{\"code\":\"RESERVATION_NOT_ACTIVE\",\"detail\":\"Rezervasyonun süresi dolmuş ya da kapanmış.\"}");

        assertThatThrownBy(() -> rezervasyonIstemcisi.onayla(9L, new OnayIstegi(5L)))
                .isInstanceOf(CakismaIstisnasi.class)
                .hasFieldOrPropertyWithValue("kod", "RESERVATION_NOT_ACTIVE");
    }

    @Test
    void shouldReportBookingUnavailable_whenBookingFails() {
        SAHTE_SUNUCU.cevapla(REZERVASYON_YOLU + "/fail", 500, "{}");

        assertThatThrownBy(() -> rezervasyonIstemcisi.basarisiz(9L))
                .isInstanceOf(BagimlilikKullanilamazIstisnasi.class)
                .hasFieldOrPropertyWithValue("kod", "BOOKING_UNAVAILABLE");
    }

    @Test
    void shouldReadBuyerInfoWithTokenForAuth_whenAskingAuth() {
        SAHTE_SUNUCU.cevapla(ALICI_YOLU, 200, """
                {"userId":42,"email":"ayse@ornek.com","firstName":"Ayşe","lastName":"Demir","phone":"+905551112233"}""");

        AliciBilgisi alici = kullaniciIstemcisi.aliciBilgisi(42L);

        assertThat(alici.firstName()).isEqualTo("Ayşe");
        assertThat(alici.phone()).isEqualTo("+905551112233");
        assertThat(alici).asString().doesNotContain("ayse@ornek.com");
        assertThat(SAHTE_SUNUCU.istekler(TOKEN_YOLU)).anySatisfy(istek -> assertThat(istek.govde())
                .contains("\"audience\":\"auth-service\""));
    }

    @Test
    void shouldMapMissingUserAndUnavailableAuth_whenAuthFails() {
        SAHTE_SUNUCU.cevapla(ALICI_YOLU, 404, "{\"code\":\"USER_NOT_FOUND\",\"detail\":\"Kullanıcı bulunamadı.\"}");
        assertThatThrownBy(() -> kullaniciIstemcisi.aliciBilgisi(42L))
                .isInstanceOf(KaynakBulunamadiIstisnasi.class)
                .hasFieldOrPropertyWithValue("kod", "USER_NOT_FOUND");

        SAHTE_SUNUCU.cevapla(ALICI_YOLU, 503, "{}");
        assertThatThrownBy(() -> kullaniciIstemcisi.aliciBilgisi(42L))
                .isInstanceOf(BagimlilikKullanilamazIstisnasi.class)
                .hasFieldOrPropertyWithValue("kod", "AUTH_UNAVAILABLE");
    }

    private static SahteServisSunucusu sunucuBaslat() {
        try {
            return new SahteServisSunucusu();
        } catch (IOException e) {
            throw new IllegalStateException("Sahte sunucu başlatılamadı", e);
        }
    }
}
