package com.sinema.booking.rezervasyon.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.booking.rezervasyon.RezervasyonEntegrasyonTesti;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class RezervasyonSorguVeBirakmaEntegrasyonTest extends RezervasyonEntegrasyonTesti {

    private static final String YOL = "/api/v1/bookings/reservations";

    // ---------- görüntüleme ----------

    @Test
    void shouldReturnOwnReservationWithTicketSnapshot_whenOwnerAsks() throws Exception {
        long kullanici = yeniKullaniciId();
        long rezervasyonId = tutVeIdAl(kullanici, yeniSeansId(), "[8, 7]");

        getir(kullanici, rezervasyonId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.movieTitle").value("Örnek Film"))
                .andExpect(jsonPath("$.cinemaName").value("Kadıköy"))
                .andExpect(jsonPath("$.hallName").value("Salon 2"))
                .andExpect(jsonPath("$.seats[*].label", contains("A-7", "A-8")))
                .andExpect(jsonPath("$.totalAmount").value("300.00"))
                .andExpect(jsonPath("$.ticketCode").doesNotExist());
    }

    @Test
    void shouldHideReservation_whenAnotherUserOrUnknownId() throws Exception {
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), yeniSeansId(), "[1]");

        getir(yeniKullaniciId(), rezervasyonId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
        getir(yeniKullaniciId(), Long.MAX_VALUE).andExpect(status().isNotFound());
    }

    @Test
    void shouldListOnlyOwnReservationsNewestFirst_withPaging() throws Exception {
        long kullanici = yeniKullaniciId();
        long ilk = tutVeIdAl(kullanici, yeniSeansId(), "[1]");
        long ikinci = tutVeIdAl(kullanici, yeniSeansId(), "[1]");
        long ucuncu = tutVeIdAl(kullanici, yeniSeansId(), "[1]");
        tutVeIdAl(yeniKullaniciId(), yeniSeansId(), "[1]");

        mockMvc.perform(get(YOL).param("size", "2").header("Authorization", yetki(kullanici)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].reservationId", contains((int) ucuncu, (int) ikinci)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get(YOL).param("page", "1").param("size", "2").header("Authorization", yetki(kullanici)))
                .andExpect(jsonPath("$.content[*].reservationId", contains((int) ilk)));
        mockMvc.perform(get(YOL).param("size", "51").header("Authorization", yetki(kullanici)))
                .andExpect(status().isBadRequest());
    }

    // ---------- bırakma (K40) ----------

    @Test
    void shouldReleaseHoldAndFreeSeatsImmediately_whenOwnerDeletesPendingReservation() throws Exception {
        long kullanici = yeniKullaniciId();
        long seansId = yeniSeansId();
        long rezervasyonId = tutVeIdAl(kullanici, seansId, "[3, 4]");
        mockMvc.perform(get("/api/v1/bookings/showtimes/{id}/seat-status", seansId)); // önbelleği doldur

        birak(kullanici, rezervasyonId).andExpect(status().isNoContent());

        assertThat(rezervasyonDurumu(rezervasyonId)).isEqualTo("RELEASED");
        mockMvc.perform(get("/api/v1/bookings/showtimes/{id}/seat-status", seansId))
                .andExpect(jsonPath("$.occupied", empty()));
        tut(yeniKullaniciId(), seansId, "[3, 4]").andExpect(status().isCreated());
    }

    @Test
    void shouldBeIdempotent_whenReleasingTwice() throws Exception {
        long kullanici = yeniKullaniciId();
        long rezervasyonId = tutVeIdAl(kullanici, yeniSeansId(), "[1]");

        birak(kullanici, rezervasyonId).andExpect(status().isNoContent());
        birak(kullanici, rezervasyonId).andExpect(status().isNoContent());
    }

    @Test
    void shouldRefuse_whenReservationConfirmedOrPaymentStartedOrExpired() throws Exception {
        long kullanici = yeniKullaniciId();
        Instant gelecek = Instant.now().plus(Duration.ofMinutes(2));
        long onayli = rezervasyonEkle(yeniSeansId(), kullanici, "CONFIRMED", gelecek, null);
        long odemede = rezervasyonEkle(yeniSeansId(), kullanici, "PENDING_PAYMENT", gelecek, null);
        jdbcTemplate.update("UPDATE reservations SET payment_window_extended = TRUE WHERE id = ?", odemede);
        long suresiDolmus = rezervasyonEkle(yeniSeansId(), kullanici, "PENDING_PAYMENT",
                Instant.now().minusSeconds(1), null);

        birak(kullanici, onayli).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_ALREADY_CONFIRMED"));
        birak(kullanici, odemede).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_IN_PROGRESS"));
        birak(kullanici, suresiDolmus).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_ACTIVE"));
        assertThat(rezervasyonDurumu(onayli)).isEqualTo("CONFIRMED");
    }

    @Test
    void shouldReturnNotFoundAndKeepHold_whenAnotherUserTriesToRelease() throws Exception {
        long seansId = yeniSeansId();
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), seansId, "[1]");

        birak(yeniKullaniciId(), rezervasyonId).andExpect(status().isNotFound());

        assertThat(rezervasyonDurumu(rezervasyonId)).isEqualTo("PENDING_PAYMENT");
        assertThat(koltukSahibi(seansId, 1)).isEqualTo(rezervasyonId);
    }

    private long tutVeIdAl(long kullanici, long seansId, String hucreler) throws Exception {
        String govde = tut(kullanici, seansId, hucreler).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(govde, "$.reservationId")).longValue();
    }

    private ResultActions getir(long kullanici, long rezervasyonId) throws Exception {
        return mockMvc.perform(get(YOL + "/{id}", rezervasyonId).header("Authorization", yetki(kullanici)));
    }

    private ResultActions birak(long kullanici, long rezervasyonId) throws Exception {
        return mockMvc.perform(delete(YOL + "/{id}", rezervasyonId).header("Authorization", yetki(kullanici)));
    }
}
