package com.sinema.payment.odeme.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.payment.OdemeEntegrasyonTesti;
import com.sinema.payment.rezervasyon.istemci.OnayIstegi;
import com.sinema.payment.rezervasyon.istemci.OnaySonucu;
import com.sinema.payment.saglayici.SorguDurumu;
import com.sinema.payment.saglayici.SorguSonucu;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Ödeme dönüşü (callback, K39, §4.8): sonuç sağlayıcıdan sorgulanır, kendi kaydımızla eşleşirse booking'e iletilir;
 * tekrar ve eşzamanlı callback tek onay üretir; sahte/değiştirilmiş sonuç bilet üretmez.
 */
class OdemeDonusKontrolcusuEntegrasyonTest extends OdemeEntegrasyonTesti {

    private static final String DONUS = "/api/v1/payments/callback/iyzico";
    private static final String SONUC_SAYFASI = "http://localhost:4200/odeme/sonuc";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void bookingOnayiniKur() {
        when(rezervasyonIstemcisi.onayla(anyLong(), any()))
                .thenAnswer(cagri -> new OnaySonucu(cagri.getArgument(0), "CONFIRMED", "ABCDEFGHJK234567"));
    }

    @Test
    void shouldConfirmBookingAndRedirectToResultPage_whenPaymentSucceeded() throws Exception {
        Baslatilan odeme = odemeBaslat();
        saglayici.sonucuKaydet(odeme.token(), true);

        donus(odeme.token())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", SONUC_SAYFASI + "?reservationId=" + odeme.rezervasyonId()));

        verify(rezervasyonIstemcisi).onayla(odeme.rezervasyonId(), new OnayIstegi(odeme.odemeId()));
        assertThat(kayit(odeme)).containsEntry("status", "SUCCEEDED").containsEntry("booking_sync_status", "DONE");
        assertThat(kayit(odeme).get("provider_payment_id")).asString().startsWith("mock-");
        assertThat(denetimKaynaklari(odeme)).containsExactly("CALLBACK", "RETRIEVE");
    }

    @Test
    void shouldFailBookingAndFreeSeats_whenPaymentDeclined() throws Exception {
        Baslatilan odeme = odemeBaslat();
        saglayici.sonucuKaydet(odeme.token(), false);

        donus(odeme.token()).andExpect(status().isFound());

        verify(rezervasyonIstemcisi).basarisiz(odeme.rezervasyonId());
        verify(rezervasyonIstemcisi, never()).onayla(anyLong(), any());
        assertThat(kayit(odeme)).containsEntry("status", "FAILED").containsEntry("booking_sync_status", "DONE");
    }

    @Test
    void shouldNotConfirmTwice_whenCallbackRepeated() throws Exception {
        Baslatilan odeme = odemeBaslat();
        saglayici.sonucuKaydet(odeme.token(), true);

        donus(odeme.token()).andExpect(status().isFound());
        donus(odeme.token()).andExpect(status().isFound());

        verify(rezervasyonIstemcisi, times(1)).onayla(eq(odeme.rezervasyonId()), any());
        verify(saglayici, times(1)).sonucuSorgula(eq(odeme.token()), any());
    }

    @Test
    void shouldConfirmExactlyOnce_whenSameCallbackArrivesConcurrently() throws Exception {
        Baslatilan odeme = odemeBaslat();
        saglayici.sonucuKaydet(odeme.token(), true);

        esZamanli(5, () -> donus(odeme.token()).andReturn().getResponse().getStatus());

        verify(rezervasyonIstemcisi, times(1)).onayla(eq(odeme.rezervasyonId()), any());
        assertThat(kayit(odeme)).containsEntry("status", "SUCCEEDED").containsEntry("booking_sync_status", "DONE");
    }

    @Test
    void shouldMarkRefundRequired_whenBookingRejectsBecauseHoldExpired() throws Exception {
        Baslatilan odeme = odemeBaslat();
        saglayici.sonucuKaydet(odeme.token(), true);
        when(rezervasyonIstemcisi.onayla(eq(odeme.rezervasyonId()), any()))
                .thenThrow(new CakismaIstisnasi("RESERVATION_NOT_ACTIVE", "Süre doldu."));

        donus(odeme.token()).andExpect(status().isFound());

        assertThat(kayit(odeme)).containsEntry("status", "REFUND_REQUIRED")
                .containsEntry("booking_sync_status", "FAILED")
                .containsEntry("failure_code", "RESERVATION_NOT_ACTIVE");
    }

    @Test
    void shouldKeepSyncPendingAndRetryOnNextCallback_whenBookingUnavailable() throws Exception {
        Baslatilan odeme = odemeBaslat();
        saglayici.sonucuKaydet(odeme.token(), true);
        when(rezervasyonIstemcisi.onayla(eq(odeme.rezervasyonId()), any()))
                .thenThrow(new BagimlilikKullanilamazIstisnasi("BOOKING_UNAVAILABLE", "x"))
                .thenReturn(new OnaySonucu(odeme.rezervasyonId(), "CONFIRMED", "ABCDEFGHJK234567"));

        donus(odeme.token()).andExpect(status().isFound());
        assertThat(kayit(odeme)).containsEntry("status", "SUCCEEDED").containsEntry("booking_sync_status", "PENDING");
        assertThat(kayit(odeme).get("booking_sync_attempts")).isEqualTo(1);
        assertThat(kayit(odeme).get("next_retry_at")).isNotNull();

        donus(odeme.token()).andExpect(status().isFound());
        assertThat(kayit(odeme)).containsEntry("booking_sync_status", "DONE");
    }

    @Test
    void shouldNotIssueTicket_whenProviderResultDoesNotMatchOurRecord() throws Exception {
        Baslatilan odeme = odemeBaslat();
        doReturn(new SorguSonucu(SorguDurumu.BASARILI, "mock-sahte", new BigDecimal("1.00"), "TRY",
                String.valueOf(odeme.rezervasyonId()), "baska-konusma", null, "{}"))
                .when(saglayici).sonucuSorgula(eq(odeme.token()), any());

        donus(odeme.token()).andExpect(status().isFound());

        verify(rezervasyonIstemcisi, never()).onayla(anyLong(), any());
        assertThat(kayit(odeme)).containsEntry("status", "REFUND_REQUIRED")
                .containsEntry("failure_code", "PROVIDER_RESULT_MISMATCH")
                .containsEntry("provider_payment_id", "mock-sahte");
    }

    @Test
    void shouldStayPendingWithoutTouchingBooking_whenUserReturnsWithoutPaying() throws Exception {
        Baslatilan odeme = odemeBaslat();

        donus(odeme.token()).andExpect(status().isFound());

        assertThat(kayit(odeme)).containsEntry("status", "PENDING");
        verify(rezervasyonIstemcisi, never()).onayla(anyLong(), any());
        verify(rezervasyonIstemcisi, never()).basarisiz(anyLong());
    }

    @Test
    void shouldRecordAndRedirectWithError_whenTokenUnknown() throws Exception {
        donus("sahte-token-123")
                .andExpect(status().isFound())
                .andExpect(header().string("Location", SONUC_SAYFASI + "?error=PAYMENT_NOT_FOUND"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_callback_logs WHERE provider_token = ? AND payment_id IS NULL",
                Integer.class, "sahte-token-123")).isEqualTo(1);
        verify(rezervasyonIstemcisi, never()).onayla(anyLong(), any());
    }

    private Baslatilan odemeBaslat() throws Exception {
        long rezervasyonId = yeniRezervasyonId();
        String govde = mockMvc.perform(post("/api/v1/payments").header("Authorization", yetki(KULLANICI))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reservationId\":" + rezervasyonId + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sayfa = JsonPath.read(govde, "$.paymentPageUrl");
        long odemeId = ((Number) JsonPath.read(govde, "$.paymentId")).longValue();
        return new Baslatilan(odemeId, rezervasyonId, sayfa.substring(sayfa.lastIndexOf('/') + 1));
    }

    private ResultActions donus(String token) throws Exception {
        return mockMvc.perform(post(DONUS).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("token", token));
    }

    private Map<String, Object> kayit(Baslatilan odeme) {
        return jdbcTemplate.queryForMap("SELECT * FROM payments WHERE id = ?", odeme.odemeId());
    }

    private List<String> denetimKaynaklari(Baslatilan odeme) {
        return jdbcTemplate.queryForList(
                "SELECT event_source FROM payment_callback_logs WHERE payment_id = ? ORDER BY id", String.class,
                odeme.odemeId());
    }

    private static void esZamanli(int adet, Callable<Integer> is) throws Exception {
        ExecutorService havuz = Executors.newFixedThreadPool(adet);
        CountDownLatch kapi = new CountDownLatch(1);
        try {
            List<Future<Integer>> sonuclar = new ArrayList<>();
            for (int i = 0; i < adet; i++) {
                sonuclar.add(havuz.submit(() -> {
                    kapi.await();
                    return is.call();
                }));
            }
            kapi.countDown();
            for (Future<Integer> sonuc : sonuclar) {
                assertThat(sonuc.get(30, TimeUnit.SECONDS)).isEqualTo(302);
            }
        } finally {
            havuz.shutdownNow();
        }
    }

    private record Baslatilan(long odemeId, long rezervasyonId, String token) {
    }
}
