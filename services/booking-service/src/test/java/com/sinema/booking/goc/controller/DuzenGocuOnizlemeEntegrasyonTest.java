package com.sinema.booking.goc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static com.sinema.booking.goc.GocTestKatalogu.YENI_DUZEN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sinema.booking.goc.GocTestKatalogu;
import com.sinema.booking.rezervasyon.RezervasyonEntegrasyonTesti;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.testdestegi.TestTokenUretici;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class DuzenGocuOnizlemeEntegrasyonTest extends RezervasyonEntegrasyonTesti {

    private static final String ADMIN = "Bearer " + TestTokenUretici.kullaniciTokeni(1, "ADMIN");
    private static final long SALON = 77;

    private long biletliSeans;
    private long biletsizSeans;

    @BeforeEach
    void hazirla() {
        biletliSeans = yeniSeansId();
        biletsizSeans = yeniSeansId();
    }

    private void katalog(String... yeniSatirlar) {
        when(katalogIstemcisi.gocKaynagi(eq(SALON), eq(YENI_DUZEN)))
                .thenReturn(GocTestKatalogu.kaynak(SALON, biletliSeans, biletsizSeans, yeniSatirlar));
    }

    private ResultActions onizle(String yetki) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/bookings/layout-migrations/preview")
                .header("Authorization", yetki)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hallId\":" + SALON + ",\"toLayoutId\":" + YENI_DUZEN + "}"));
    }

    /** Koltuğu tutar ve satılmış yapar (onay akışının sonucu; burada konu değil). */
    private long satinAl(long seansId, long hucreId) throws Exception {
        long rezervasyon = tutVeIdAl(yeniKullaniciId(), seansId, "[" + hucreId + "]");
        jdbcTemplate.update("UPDATE seat_bookings SET status = 'SOLD', hold_expires_at = NULL WHERE reservation_id = ?",
                rezervasyon);
        jdbcTemplate.update("UPDATE reservations SET status = 'CONFIRMED' WHERE id = ?", rezervasyon);
        return rezervasyon;
    }

    @Test
    void shouldReportLabelChangeAndAdjacentReplacement_forSoldAndHeldSeats_withoutWritingAnything() throws Exception {
        // Yeni düzen: 1. satır kalktı; eski 2. satır yerinde (artık A sırası), altına iki koltuklu yeni sıra
        katalog("PPPP", "....", "KKKK", "KK..");
        long satilan = satinAl(biletliSeans, 911 + 1);
        long tutulan = tutVeIdAl(yeniKullaniciId(), biletliSeans, "[902, 903]");

        onizle(ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicable").value(true))
                .andExpect(jsonPath("$.summary.migratedShowtimes").value(2))
                .andExpect(jsonPath("$.summary.seats").value(3))
                .andExpect(jsonPath("$.summary.unchanged").value(0))
                .andExpect(jsonPath("$.summary.labelChanged").value(1))
                .andExpect(jsonPath("$.summary.replaced").value(2))
                .andExpect(jsonPath("$.summary.unresolvedReservations").value(0))
                .andExpect(jsonPath("$.showtimes", hasSize(1)))
                .andExpect(jsonPath("$.showtimes[0].showtimeId").value(biletliSeans))
                .andExpect(jsonPath("$.showtimes[0].changes[?(@.reservationId == " + satilan + ")].newLabel").value("A-2"))
                .andExpect(jsonPath("$.showtimes[0].changes[?(@.reservationId == " + satilan + ")].reason")
                        .value("LABEL_CHANGED"))
                // Kalkan A-2, A-3 için en yakın bitişik boş ikili: yeni A-3, A-4 (A-2 satılan biletin)
                .andExpect(jsonPath("$.showtimes[0].changes[?(@.reservationId == " + tutulan + ")].newLabel",
                        containsInAnyOrder("A-3", "A-4")));

        // Önizleme yazmaz: koltuklar eski hücrelerinde
        assertThat(koltukSahibi(biletliSeans, 902)).isEqualTo(tutulan);
        assertThat(koltukSahibi(biletliSeans, 912)).isEqualTo(satilan);
    }

    @Test
    void shouldMarkNotApplicable_whenReservationCannotBeSeatedTogether() throws Exception {
        katalog("PPPP", "....", "K.K.");
        long tutulan = tutVeIdAl(yeniKullaniciId(), biletliSeans, "[902, 903]");

        onizle(ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicable").value(false))
                .andExpect(jsonPath("$.summary.unresolvedReservations").value(1))
                .andExpect(jsonPath("$.showtimes[0].unresolved[0].reservationId").value(tutulan))
                .andExpect(jsonPath("$.showtimes[0].unresolved[0].oldLabels", containsInAnyOrder("A-2", "A-3")));
    }

    @Test
    void shouldPassThroughCatalogConflict_andRejectNonAdmin() throws Exception {
        when(katalogIstemcisi.gocKaynagi(anyLong(), anyLong()))
                .thenThrow(new CakismaIstisnasi("LAYOUT_ALREADY_PUBLISHED", "Bu düzen zaten yayımlanmış."));

        onizle(ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAYOUT_ALREADY_PUBLISHED"));
        onizle(yetki(yeniKullaniciId())).andExpect(status().isForbidden());
    }
}
