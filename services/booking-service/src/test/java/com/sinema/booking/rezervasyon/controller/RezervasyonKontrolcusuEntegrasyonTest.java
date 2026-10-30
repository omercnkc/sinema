package com.sinema.booking.rezervasyon.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.booking.rezervasyon.RezervasyonEntegrasyonTesti;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class RezervasyonKontrolcusuEntegrasyonTest extends RezervasyonEntegrasyonTesti {

    private static final String YOL = "/api/v1/bookings/reservations";
    private static final long KULLANICI = 41;

    @Test
    void shouldHoldSeatsWithSnapshotAndExpiry_whenSeatsFree() throws Exception {
        long seansId = yeniSeansId();
        Instant once = Instant.now();

        String govde = tut(KULLANICI, seansId, "[12, 11]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.seats[*].label", contains("A-11", "A-12")))
                .andExpect(jsonPath("$.totalAmount").value("300.00"))
                .andReturn().getResponse().getContentAsString();

        Instant bitis = Instant.parse(JsonPath.read(govde, "$.expiresAt"));
        Instant sunucuSaati = Instant.parse(JsonPath.read(govde, "$.serverTime"));
        assertThat(Duration.between(sunucuSaati, bitis)).isEqualTo(Duration.ofMinutes(3));
        assertThat(sunucuSaati).isAfterOrEqualTo(once.truncatedTo(ChronoUnit.MILLIS));
        assertThat(bitis).as("DB ile aynı hassasiyet").isEqualTo(bitis.truncatedTo(ChronoUnit.MILLIS));
        long rezervasyonId = ((Number) JsonPath.read(govde, "$.reservationId")).longValue();
        assertThat(koltukSahibi(seansId, 11)).isEqualTo(rezervasyonId);
        assertThat(koltukSahibi(seansId, 12)).isEqualTo(rezervasyonId);
        assertThat(jdbcTemplate.queryForObject("SELECT movie_title FROM reservations WHERE id = ?", String.class,
                rezervasyonId)).isEqualTo("Örnek Film");
    }

    @Test
    void shouldCapExpiryAtSalesClose_whenSalesCloseSooner() throws Exception {
        long seansId = yeniSeansId();
        Instant kapanis = Instant.now().plus(Duration.ofSeconds(60)).truncatedTo(ChronoUnit.MILLIS);
        satisKapanislari.put(seansId, kapanis);

        tut(KULLANICI, seansId, "[1]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").value(kapanis.toString()));
    }

    @Test
    void shouldRejectWithSalesClosed_whenSalesCloseHasPassed() throws Exception {
        long seansId = yeniSeansId();
        satisKapanislari.put(seansId, Instant.now().minusSeconds(1));

        tut(KULLANICI, seansId, "[1]")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SALES_CLOSED"));
    }

    @Test
    void shouldReturnUnavailableCells_whenSeatAlreadyHeldByAnotherUser() throws Exception {
        long seansId = yeniSeansId();
        tut(99, seansId, "[5]").andExpect(status().isCreated());

        tut(KULLANICI, seansId, "[4, 5]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEATS_UNAVAILABLE"))
                .andExpect(jsonPath("$.unavailableCellIds", contains(5)));
        assertThat(koltukSahibi(seansId, 4)).as("hepsi ya da hiçbiri").isNull();
    }

    @Test
    void shouldTakeOverExpiredHold_whenPreviousHolderDidNotPay() throws Exception {
        long seansId = yeniSeansId();
        suresiDolmusTutmaEkle(seansId, 7, 99);

        String govde = tut(KULLANICI, seansId, "[7]").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long rezervasyonId = ((Number) JsonPath.read(govde, "$.reservationId")).longValue();
        assertThat(koltukSahibi(seansId, 7)).isEqualTo(rezervasyonId);
    }

    @Test
    void shouldRejectWithExistingReservationId_whenUserAlreadyHasActiveHoldForShowtime() throws Exception {
        long seansId = yeniSeansId();
        String ilk = tut(KULLANICI, seansId, "[1]").andReturn().getResponse().getContentAsString();

        tut(KULLANICI, seansId, "[2]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_RESERVATION_EXISTS"))
                .andExpect(jsonPath("$.reservationId").value((Object) JsonPath.read(ilk, "$.reservationId")));
    }

    @Test
    void shouldExpireOldPendingAndCreateNew_whenUsersPreviousHoldExpired() throws Exception {
        long seansId = yeniSeansId();
        long eski = bekleyenRezervasyonEkle(seansId, KULLANICI, Timestamp.from(Instant.now().minusSeconds(5)));

        tut(KULLANICI, seansId, "[3]").andExpect(status().isCreated());

        assertThat(rezervasyonDurumu(eski)).isEqualTo("EXPIRED");
    }

    @Test
    void shouldRejectInvalidCells_whenCatalogSaysTheyAreNotSeats() throws Exception {
        long seansId = yeniSeansId();
        gecersizHucreler.add(900L);

        tut(KULLANICI, seansId, "[1, 900]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SEATS"))
                .andExpect(jsonPath("$.invalidCellIds", contains(900)));
        assertThat(seansinRezervasyonSayisi(seansId)).isZero();
    }

    @Test
    void shouldRejectTooManyOrDuplicateSeats_beforeCallingCatalog() throws Exception {
        long seansId = yeniSeansId();

        tut(KULLANICI, seansId, "[1, 2, 3, 4, 5, 6]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOO_MANY_SEATS"));
        tut(KULLANICI, seansId, "[1, 1]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SEATS"));
        tut(KULLANICI, seansId, "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldRequireLogin_whenGuestTriesToHold() throws Exception {
        mockMvc.perform(post(YOL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showtimeId\":1,\"seatCellIds\":[1]}"))
                .andExpect(status().isUnauthorized());
    }

}
