package com.sinema.booking.rezervasyon.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.booking.rezervasyon.RezervasyonEntegrasyonTesti;
import com.sinema.testdestegi.TestTokenUretici;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** payment → booking iç uçları (planlama §4.5, §4.10-5, K32): gerçek servis token'ıyla, gerçek MySQL üzerinde. */
class RezervasyonIcKontrolcusuEntegrasyonTest extends RezervasyonEntegrasyonTesti {

    private static final String PAYMENT_TOKENI =
            "Bearer " + TestTokenUretici.servisTokeni("payment-service", "booking-service");
    private static final String YOL = "/internal/v1/bookings/reservations/{id}";

    @Test
    void shouldReturnServerSideAmountAndOwner_whenPaymentAsksInfo() throws Exception {
        long kullanici = yeniKullaniciId();
        long rezervasyonId = tutVeIdAl(kullanici, yeniSeansId(), "[1, 2]");

        ic(get(YOL + "/payment-info", rezervasyonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(kullanici))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value("300.00"))
                .andExpect(jsonPath("$.paymentWindowExtended").value(false));
    }

    @Test
    void shouldSellAllSeatsAndIssueTicketCode_whenConfirmedInTime() throws Exception {
        long seansId = yeniSeansId();
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), seansId, "[5, 6]");

        String govde = onayla(rezervasyonId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();

        String biletKodu = JsonPath.read(govde, "$.ticketCode");
        assertThat(biletKodu).matches("[0-9A-HJKMNP-TV-Z]{16}");
        assertThat(koltukDurumlari(rezervasyonId)).containsOnly("SOLD");
        mockMvc.perform(get("/api/v1/bookings/showtimes/{id}/seat-status", seansId))
                .andExpect(jsonPath("$.occupied[*].status", contains("SOLD", "SOLD")));
    }

    @Test
    void shouldReturnSameTicketCode_whenConfirmArrivesTwice() throws Exception {
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), yeniSeansId(), "[1]");

        String ilk = onayla(rezervasyonId).andReturn().getResponse().getContentAsString();
        String ikinci = onayla(rezervasyonId).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(ikinci, "$.ticketCode")).isEqualTo(JsonPath.read(ilk, "$.ticketCode"));
    }

    @Test
    void shouldRejectConfirmAndSellNothing_whenHoldExpired() throws Exception {
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), yeniSeansId(), "[1, 2]");
        suresiniDoldur(rezervasyonId);

        onayla(rezervasyonId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_ACTIVE"));

        assertThat(koltukDurumlari(rezervasyonId)).containsOnly("HELD");
        assertThat(rezervasyonDurumu(rezervasyonId)).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void shouldExtendHoldOnceForReservationAndSeats_whenPaymentStarts() throws Exception {
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), yeniSeansId(), "[1, 2]");
        Instant ilkBitis = bitis(rezervasyonId);

        String govde = ic(post(YOL + "/extend-payment-window", rezervasyonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentWindowExtended").value(true))
                .andReturn().getResponse().getContentAsString();
        ic(post(YOL + "/extend-payment-window", rezervasyonId)).andExpect(status().isOk());

        Instant yeniBitis = Instant.parse(JsonPath.read(govde, "$.expiresAt"));
        assertThat(yeniBitis).isEqualTo(ilkBitis.plus(Duration.ofMinutes(2)));
        assertThat(bitis(rezervasyonId)).as("ikinci uzatma bir şey yapmaz").isEqualTo(yeniBitis);
        assertThat(koltukBitisleri(rezervasyonId)).containsOnly(Timestamp.from(yeniBitis));
    }

    @Test
    void shouldCapExtensionAtSalesClose_whenSalesCloseIsSooner() throws Exception {
        long seansId = yeniSeansId();
        Instant kapanis = Instant.now().plus(Duration.ofSeconds(200)).truncatedTo(ChronoUnit.MILLIS);
        satisKapanislari.put(seansId, kapanis);
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), seansId, "[1]");

        ic(post(YOL + "/extend-payment-window", rezervasyonId))
                .andExpect(jsonPath("$.expiresAt").value(kapanis.toString()));
    }

    @Test
    void shouldRejectExtension_whenHoldExpired() throws Exception {
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), yeniSeansId(), "[1]");
        suresiniDoldur(rezervasyonId);

        ic(post(YOL + "/extend-payment-window", rezervasyonId)).andExpect(status().isConflict());
    }

    @Test
    void shouldFailReservationAndFreeSeats_whenPaymentRejected() throws Exception {
        long seansId = yeniSeansId();
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), seansId, "[3]");

        ic(post(YOL + "/fail", rezervasyonId)).andExpect(status().isNoContent());
        ic(post(YOL + "/fail", rezervasyonId)).andExpect(status().isNoContent());

        assertThat(rezervasyonDurumu(rezervasyonId)).isEqualTo("FAILED");
        assertThat(koltukSahibi(seansId, 3)).isNull();
    }

    @Test
    void shouldRefuseToFail_whenAlreadyConfirmed() throws Exception {
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), yeniSeansId(), "[1]");
        onayla(rezervasyonId);

        ic(post(YOL + "/fail", rezervasyonId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_ALREADY_CONFIRMED"));
    }

    @Test
    void shouldForbidInternalEndpoints_forUserTokenAndOtherAudience() throws Exception {
        long kullanici = yeniKullaniciId();
        long rezervasyonId = tutVeIdAl(kullanici, yeniSeansId(), "[1]");

        mockMvc.perform(post(YOL + "/confirm", rezervasyonId).header("Authorization", yetki(kullanici))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentId\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(YOL + "/payment-info", rezervasyonId).header("Authorization",
                        "Bearer " + TestTokenUretici.servisTokeni("payment-service", "catalog-service")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFound_whenReservationMissing() throws Exception {
        ic(get(YOL + "/payment-info", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    private ResultActions onayla(long rezervasyonId) throws Exception {
        return ic(post(YOL + "/confirm", rezervasyonId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"paymentId\":77}"));
    }

    private ResultActions ic(MockHttpServletRequestBuilder istek) throws Exception {
        return mockMvc.perform(istek.header("Authorization", PAYMENT_TOKENI));
    }

    private void suresiniDoldur(long rezervasyonId) {
        Timestamp gecmis = Timestamp.from(Instant.now().minusSeconds(1));
        jdbcTemplate.update("UPDATE reservations SET expires_at = ? WHERE id = ?", gecmis, rezervasyonId);
        jdbcTemplate.update("UPDATE seat_bookings SET hold_expires_at = ? WHERE reservation_id = ?", gecmis,
                rezervasyonId);
    }

    private Instant bitis(long rezervasyonId) {
        return jdbcTemplate.queryForObject("SELECT expires_at FROM reservations WHERE id = ?", Timestamp.class,
                rezervasyonId).toInstant();
    }

    private List<String> koltukDurumlari(long rezervasyonId) {
        return jdbcTemplate.queryForList("SELECT status FROM seat_bookings WHERE reservation_id = ?", String.class,
                rezervasyonId);
    }

    private List<Timestamp> koltukBitisleri(long rezervasyonId) {
        return jdbcTemplate.queryForList("SELECT hold_expires_at FROM seat_bookings WHERE reservation_id = ?",
                Timestamp.class, rezervasyonId);
    }
}
