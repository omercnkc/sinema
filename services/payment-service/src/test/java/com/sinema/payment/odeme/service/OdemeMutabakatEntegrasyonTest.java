package com.sinema.payment.odeme.service;

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

import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.payment.OdemeEntegrasyonTesti;
import com.sinema.payment.rezervasyon.istemci.OnayIstegi;
import com.sinema.payment.rezervasyon.istemci.OnaySonucu;
import com.sinema.payment.saglayici.IadeSonucu;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Mutabakat (planlama §4.8): booking senkronu yeniden denenir, callback'i gelmeyen ödemeler sağlayıcıdan kapatılır,
 * terk edilen ödeme FAILED olur (booking'e ret gönderilmez, K29), iade gerekenler iade edilir, eski kayıtlar silinir.
 * Zaman, kayıtların zaman alanları geriye çekilerek simüle edilir.
 */
class OdemeMutabakatEntegrasyonTest extends OdemeEntegrasyonTesti {

    @Autowired
    private OdemeMutabakatServisi mutabakat;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void bookingOnayiniKur() {
        when(rezervasyonIstemcisi.onayla(anyLong(), any()))
                .thenAnswer(cagri -> new OnaySonucu(cagri.getArgument(0), "CONFIRMED", "ABCDEFGHJK234567"));
    }

    // ---------- booking senkronunu yeniden deneme ----------

    @Test
    void shouldRetryBookingConfirm_whenRetryTimeArrived() throws Exception {
        Baslatilan odeme = odemeBaslat();
        saglayici.sonucuKaydet(odeme.token(), true);
        when(rezervasyonIstemcisi.onayla(eq(odeme.rezervasyonId()), any()))
                .thenThrow(new BagimlilikKullanilamazIstisnasi("BOOKING_UNAVAILABLE", "x"))
                .thenReturn(new OnaySonucu(odeme.rezervasyonId(), "CONFIRMED", "ABCDEFGHJK234567"));
        donus(odeme.token());
        assertThat(kayit(odeme)).containsEntry("booking_sync_status", "PENDING");

        mutabakat.senkronlariYenidenDene();
        assertThat(kayit(odeme)).as("deneme zamanı gelmedi").containsEntry("booking_sync_status", "PENDING");

        geriCek(odeme, "next_retry_at", Duration.ofMinutes(2));
        mutabakat.senkronlariYenidenDene();
        assertThat(kayit(odeme)).containsEntry("booking_sync_status", "DONE");
        verify(rezervasyonIstemcisi, times(2))
                .onayla(odeme.rezervasyonId(), new OnayIstegi(odeme.odemeId()));
    }

    // ---------- callback'i gelmeyen ödemeler ----------

    @Test
    void shouldCompletePaidPaymentAndConfirmBooking_whenCallbackNeverArrived() throws Exception {
        Baslatilan odeme = odemeBaslat();
        saglayici.sonucuKaydet(odeme.token(), true); // kullanıcı ödedi ama tarayıcıyı kapattı

        mutabakat.bekleyenOdemeleriKapat();
        assertThat(kayit(odeme)).as("henüz sakin değil (callback gelebilir)").containsEntry("status", "PENDING");

        geriCek(odeme, "updated_at", Duration.ofMinutes(5));
        mutabakat.bekleyenOdemeleriKapat();

        assertThat(kayit(odeme)).containsEntry("status", "SUCCEEDED").containsEntry("booking_sync_status", "DONE");
        verify(rezervasyonIstemcisi).onayla(odeme.rezervasyonId(), new OnayIstegi(odeme.odemeId()));
    }

    @Test
    void shouldMarkAbandonedWithoutFailingBooking_whenTokenExpiredAndNothingPaid() throws Exception {
        Baslatilan odeme = odemeBaslat();
        geriCek(odeme, "updated_at", Duration.ofMinutes(40));
        geriCek(odeme, "provider_token_expires_at", Duration.ofMinutes(31));

        mutabakat.bekleyenOdemeleriKapat();

        assertThat(kayit(odeme)).containsEntry("status", "FAILED")
                .containsEntry("failure_code", "PAYMENT_ABANDONED")
                .containsEntry("booking_sync_status", "NOT_NEEDED");
        verify(rezervasyonIstemcisi, never()).basarisiz(anyLong());
    }

    @Test
    void shouldKeepWaiting_whenTokenStillValidAndNothingPaid() throws Exception {
        Baslatilan odeme = odemeBaslat();
        geriCek(odeme, "updated_at", Duration.ofMinutes(5));

        mutabakat.bekleyenOdemeleriKapat();

        assertThat(kayit(odeme)).containsEntry("status", "PENDING");
    }

    @Test
    void shouldIgnorePendingPaymentsOfAnotherProvider() {
        long rezervasyonId = yeniRezervasyonId();
        Timestamp eski = Timestamp.from(Instant.now().minus(Duration.ofHours(1)));
        jdbcTemplate.update("""
                INSERT INTO payments (reservation_id, user_id, provider, status, amount, currency, conversation_id,
                                      provider_token, provider_token_expires_at, booking_sync_status, created_at, updated_at)
                VALUES (?, 42, 'IYZICO', 'PENDING', 300.00, 'TRY', ?, ?, ?, 'NOT_NEEDED', ?, ?)""",
                rezervasyonId, "baska-" + rezervasyonId, "iyzico-token-" + rezervasyonId, eski, eski, eski);

        mutabakat.bekleyenOdemeleriKapat();

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM payments WHERE reservation_id = ?", String.class,
                rezervasyonId)).isEqualTo("PENDING");
    }

    // ---------- iade ----------

    @Test
    void shouldRefundPayment_whenBookingRejectedConfirmation() throws Exception {
        Baslatilan odeme = odemeBaslat();
        saglayici.sonucuKaydet(odeme.token(), true);
        when(rezervasyonIstemcisi.onayla(eq(odeme.rezervasyonId()), any()))
                .thenThrow(new CakismaIstisnasi("RESERVATION_NOT_ACTIVE", "Süre doldu."));
        donus(odeme.token());
        assertThat(kayit(odeme)).containsEntry("status", "REFUND_REQUIRED");

        mutabakat.iadeleriYap();

        assertThat(kayit(odeme)).containsEntry("status", "REFUNDED");
        verify(saglayici).iadeEt(eq((String) kayit(odeme).get("provider_payment_id")), any(), any());
    }

    @Test
    void shouldRetryRefundLater_whenProviderRejects() throws Exception {
        Baslatilan odeme = odemeBaslat();
        saglayici.sonucuKaydet(odeme.token(), true);
        when(rezervasyonIstemcisi.onayla(eq(odeme.rezervasyonId()), any()))
                .thenThrow(new CakismaIstisnasi("RESERVATION_NOT_ACTIVE", "Süre doldu."));
        donus(odeme.token());
        doReturn(IadeSonucu.reddedildi("5093")).doCallRealMethod().when(saglayici).iadeEt(any(), any(), any());

        mutabakat.iadeleriYap();
        assertThat(kayit(odeme)).containsEntry("status", "REFUND_REQUIRED").containsEntry("failure_code", "5093");
        assertThat(kayit(odeme).get("next_retry_at")).isNotNull();

        mutabakat.iadeleriYap();
        assertThat(kayit(odeme)).as("bekleme süresi dolmadı").containsEntry("status", "REFUND_REQUIRED");

        geriCek(odeme, "next_retry_at", Duration.ofMinutes(11));
        mutabakat.iadeleriYap();
        assertThat(kayit(odeme)).containsEntry("status", "REFUNDED");
    }

    // ---------- eski kayıt temizliği ----------

    @Test
    void shouldDeleteOnlyRecordsOlderThanRetention() {
        Timestamp eski = Timestamp.from(Instant.now().minus(Duration.ofDays(91)));
        Timestamp yeni = Timestamp.from(Instant.now().minus(Duration.ofDays(89)));
        jdbcTemplate.update("INSERT INTO payment_callback_logs (provider_token, event_source, received_at) "
                + "VALUES ('temizlik-eski', 'CALLBACK', ?), ('temizlik-yeni', 'CALLBACK', ?)", eski, yeni);

        mutabakat.eskiKayitlariSil();

        assertThat(jdbcTemplate.queryForList("SELECT provider_token FROM payment_callback_logs "
                + "WHERE provider_token LIKE 'temizlik-%'", String.class)).containsExactly("temizlik-yeni");
    }

    private void donus(String token) throws Exception {
        mockMvc.perform(post("/api/v1/payments/callback/iyzico").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token));
    }

    /** Zaman alanını geriye çeker (süre geçmiş gibi). */
    private void geriCek(Baslatilan odeme, String kolon, Duration sure) {
        jdbcTemplate.update("UPDATE payments SET " + kolon + " = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(sure)), odeme.odemeId());
    }

    private Map<String, Object> kayit(Baslatilan odeme) {
        return jdbcTemplate.queryForMap("SELECT * FROM payments WHERE id = ?", odeme.odemeId());
    }
}
