package com.sinema.booking.goc.controller;

import static com.sinema.booking.goc.GocTestKatalogu.YENI_DUZEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sinema.booking.goc.GocTestKatalogu;
import com.sinema.booking.katalog.istemci.KatalogEtkinlestirmeSonucu;
import com.sinema.booking.rezervasyon.RezervasyonEntegrasyonTesti;
import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.testdestegi.TestTokenUretici;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Düzen göçünün uygulanması (K35, K51): gerçek MySQL, sahte catalog. Salon 2: sahte catalog bütün seansları
 * salon 2'de gösterir, böylece bakım kilidinin koltuk tutmayı engellediği de sınanır.
 */
class DuzenGocuUygulamaEntegrasyonTest extends RezervasyonEntegrasyonTesti {

    private static final String ADMIN = "Bearer " + TestTokenUretici.kullaniciTokeni(1, "ADMIN");
    private static final String PAYMENT = "Bearer " + TestTokenUretici.servisTokeni("payment-service", "booking-service");
    private static final long SALON = 2;

    private long biletliSeans;
    private long biletsizSeans;
    private long satinAlan;
    private long satilan;
    private long tutan;
    private long tutulan;

    @BeforeEach
    void hazirla() throws Exception {
        biletliSeans = yeniSeansId();
        biletsizSeans = yeniSeansId();
        satinAlan = yeniKullaniciId();
        satilan = satinAl(satinAlan, biletliSeans, 912);
        tutan = yeniKullaniciId();
        tutulan = tutVeIdAl(tutan, biletliSeans, "[902, 903]");
    }

    /** Askıya alma testi kilidi bilerek bırakır; aynı salonu kullanan diğer testler etkilenmesin. */
    @AfterEach
    void kilidiTemizle() {
        jdbcTemplate.update("DELETE FROM hall_maintenance_locks WHERE hall_id = ?", SALON);
    }

    /** Yeni düzen: 1. satır kalktı; eski 2. satır yerinde (artık A), altına iki koltuklu yeni sıra (B). */
    private void katalog(String... yeniSatirlar) {
        when(katalogIstemcisi.gocKaynagi(eq(SALON), eq(YENI_DUZEN)))
                .thenReturn(GocTestKatalogu.kaynak(SALON, biletliSeans, biletsizSeans, yeniSatirlar));
    }

    private ResultActions uygula(long hedef) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/bookings/layout-migrations")
                .header("Authorization", ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hallId\":" + SALON + ",\"toLayoutId\":" + hedef + "}"));
    }

    private long satinAl(long kullaniciId, long seansId, long hucreId) throws Exception {
        long rezervasyon = tutVeIdAl(kullaniciId, seansId, "[" + hucreId + "]");
        jdbcTemplate.update("UPDATE seat_bookings SET status = 'SOLD', hold_expires_at = NULL WHERE reservation_id = ?",
                rezervasyon);
        jdbcTemplate.update("UPDATE reservations SET status = 'CONFIRMED' WHERE id = ?", rezervasyon);
        return rezervasyon;
    }

    private List<String> etiketler(long rezervasyonId) {
        return jdbcTemplate.queryForList(
                "SELECT seat_label FROM reservation_seats WHERE reservation_id = ? ORDER BY seat_label", String.class,
                rezervasyonId);
    }

    private List<String> bildirimler(long rezervasyonId) {
        return jdbcTemplate.queryForList("SELECT message FROM notifications WHERE reservation_id = ?", String.class,
                rezervasyonId);
    }

    private String sonGocDurumu() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM layout_migrations WHERE hall_id = ? ORDER BY id DESC LIMIT 1", String.class, SALON);
    }

    private boolean salonKilitli() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hall_maintenance_locks WHERE hall_id = ?",
                Integer.class, SALON) > 0;
    }

    private long yeni(int satir, int sutun) {
        return GocTestKatalogu.hucre(YENI_DUZEN, satir, sutun);
    }

    @Test
    void shouldMoveSoldAndHeldSeatsActivateAndNotify_whenApplied() throws Exception {
        katalog("PPPP", "....", "KKKK", "KK..");
        when(katalogIstemcisi.etkinlestir(eq(SALON), eq(YENI_DUZEN), any()))
                .thenReturn(new KatalogEtkinlestirmeSonucu(YENI_DUZEN, 2, 2));

        uygula(YENI_DUZEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.layoutVersion").value(2))
                .andExpect(jsonPath("$.migratedShowtimes").value(2))
                .andExpect(jsonPath("$.report.summary.replaced").value(2));

        assertThat(koltukSahibi(biletliSeans, yeni(2, 1))).isEqualTo(satilan);
        assertThat(koltukSahibi(biletliSeans, 912)).isNull();
        assertThat(koltukSahibi(biletliSeans, yeni(2, 2))).isEqualTo(tutulan);
        assertThat(koltukSahibi(biletliSeans, yeni(2, 3))).isEqualTo(tutulan);
        assertThat(etiketler(satilan)).containsExactly("A-2");
        assertThat(etiketler(tutulan)).containsExactly("A-3", "A-4");
        // Bildirim biletin üzerindeki etiketi gösterir (sahte catalog bileti "A-{hücre}" etiketiyle kesti)
        assertThat(bildirimler(satilan)).singleElement().asString().contains("A-912 → A-2");
        assertThat(bildirimler(tutulan)).singleElement().asString().contains("A-902 → A-3, A-903 → A-4");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM seat_migration_audit WHERE reservation_id IN (?, ?)",
                Integer.class, satilan, tutulan)).isEqualTo(3);
        assertThat(sonGocDurumu()).isEqualTo("COMPLETED");
        assertThat(salonKilitli()).isFalse();
        verify(katalogIstemcisi).etkinlestir(eq(SALON), eq(YENI_DUZEN),
                argThat(istek -> istek.showtimeIds().equals(List.of(biletliSeans))));

        // K51: taşınan bekleyen işlemin ödemesi yeni koltuklarda tamamlanır
        mockMvc.perform(post("/internal/v1/bookings/reservations/{id}/confirm", tutulan).header("Authorization", PAYMENT)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentId\":77}"))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForList("SELECT status FROM seat_bookings WHERE reservation_id = ?", String.class,
                tutulan)).containsOnly("SOLD");

        // Bilet sayfası bandı: tamamlanan göçteki değişiklikler, biletin eski etiketinden yenisine
        mockMvc.perform(get("/api/v1/bookings/reservations/{id}", tutulan).header("Authorization", yetki(tutan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatChanges[*].oldLabel", containsInAnyOrder("A-902", "A-903")))
                .andExpect(jsonPath("$.seatChanges[*].newLabel", containsInAnyOrder("A-3", "A-4")))
                .andExpect(jsonPath("$.seatChanges[0].reason").value("REPLACEMENT"))
                .andExpect(jsonPath("$.seats[*].label", containsInAnyOrder("A-3", "A-4")));
        mockMvc.perform(get("/api/v1/bookings/reservations/{id}", satilan).header("Authorization", yetki(satinAlan)))
                .andExpect(jsonPath("$.seatChanges[0].reason").value("LABEL_CHANGED"));
        // QR doğrulama yeni etiketleri gösterir (bilet snapshot'ı göçte güncellendi)
        String biletKodu = jdbcTemplate.queryForObject("SELECT ticket_code FROM reservations WHERE id = ?",
                String.class, tutulan);
        mockMvc.perform(get("/api/v1/tickets/{kod}", biletKodu))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats", containsInAnyOrder("A-3", "A-4")));
    }

    @Test
    void shouldChangeNothingAndUnlock_whenReservationCannotBeSeated() throws Exception {
        katalog("PPPP", "....", "K.K.");

        uygula(YENI_DUZEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MIGRATION_NOT_APPLICABLE"))
                .andExpect(jsonPath("$.report.applicable").value(false))
                .andExpect(jsonPath("$.report.showtimes[0].unresolved[0].reservationId").value(tutulan));

        assertThat(koltukSahibi(biletliSeans, 902)).isEqualTo(tutulan);
        assertThat(etiketler(satilan)).containsExactly("A-912");
        assertThat(sonGocDurumu()).isEqualTo("ROLLED_BACK");
        assertThat(salonKilitli()).isFalse();
        verify(katalogIstemcisi, never()).etkinlestir(any(), any(), any());
    }

    @Test
    void shouldRestoreSeatsWithoutNotifying_whenCatalogRejectsActivation() throws Exception {
        katalog("PPPP", "....", "KKKK", "KK..");
        when(katalogIstemcisi.etkinlestir(eq(SALON), eq(YENI_DUZEN), any())).thenThrow(new CakismaIstisnasi(
                "MIGRATION_SHOWTIMES_CHANGED", "Göç planındaki bazı seanslar başladı."));

        uygula(YENI_DUZEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MIGRATION_SHOWTIMES_CHANGED"));

        assertThat(koltukSahibi(biletliSeans, 912)).isEqualTo(satilan);
        assertThat(koltukSahibi(biletliSeans, 902)).isEqualTo(tutulan);
        assertThat(koltukSahibi(biletliSeans, yeni(2, 1))).isNull();
        assertThat(etiketler(satilan)).containsExactly("A-912");
        assertThat(etiketler(tutulan)).containsExactly("A-902", "A-903");
        assertThat(bildirimler(satilan)).isEmpty();
        // Geri alınan göçün denetim kaydı kalır ama bilet sayfasında değişiklik görünmez
        mockMvc.perform(get("/api/v1/bookings/reservations/{id}", satilan).header("Authorization", yetki(satinAlan)))
                .andExpect(jsonPath("$.seatChanges").isEmpty());
        assertThat(sonGocDurumu()).isEqualTo("ROLLED_BACK");
        assertThat(salonKilitli()).isFalse();
    }

    @Test
    void shouldKeepLockAndBlockHolds_whenCatalogUnreachable_thenResumeOnRetry() throws Exception {
        katalog("PPPP", "....", "KKKK", "KK..");
        when(katalogIstemcisi.etkinlestir(eq(SALON), eq(YENI_DUZEN), any()))
                .thenThrow(new BagimlilikKullanilamazIstisnasi("CATALOG_UNAVAILABLE", "catalog yok"))
                .thenReturn(new KatalogEtkinlestirmeSonucu(YENI_DUZEN, 2, 2));

        uygula(YENI_DUZEN)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("MIGRATION_PENDING"));

        // Biletler taşınmış kalır (catalog etkinleştirmiş olabilir), salon kilitli: yeni koltuk tutulamaz
        assertThat(koltukSahibi(biletliSeans, yeni(2, 1))).isEqualTo(satilan);
        assertThat(sonGocDurumu()).isEqualTo("FAILED");
        assertThat(salonKilitli()).isTrue();
        tut(yeniKullaniciId(), biletsizSeans, "[1]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HALL_UNDER_MAINTENANCE"));
        uygula(YENI_DUZEN + 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MIGRATION_IN_PROGRESS"));

        uygula(YENI_DUZEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.report").value(nullValue()));
        assertThat(bildirimler(tutulan)).hasSize(1);
        assertThat(sonGocDurumu()).isEqualTo("COMPLETED");
        assertThat(salonKilitli()).isFalse();
    }
}
