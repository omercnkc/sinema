package com.sinema.booking.rezervasyon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.booking.rezervasyon.RezervasyonEntegrasyonTesti;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** K29 (planlama §4.7, §4.10-6): son 30 dk'da 3 terk → son terkten 10 dk sonrasına kadar 429. */
class TerkKorumasiEntegrasyonTest extends RezervasyonEntegrasyonTesti {

    @Test
    void shouldRejectWithRetryAfter_whenThreeHoldsReleasedRecently() throws Exception {
        long kullanici = yeniKullaniciId();
        Instant simdi = Instant.now();
        birakilmis(kullanici, simdi.minus(Duration.ofMinutes(20)));
        birakilmis(kullanici, simdi.minus(Duration.ofMinutes(10)));
        Instant sonTerk = simdi.minus(Duration.ofMinutes(2));
        birakilmis(kullanici, sonTerk);

        MvcResult sonuc = tut(kullanici, yeniSeansId(), "[1]")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("HOLD_COOLDOWN"))
                .andReturn();

        long beklemeSaniyesi = Long.parseLong(sonuc.getResponse().getHeader("Retry-After"));
        assertThat(beklemeSaniyesi).as("son terk + 10 dk - şimdi ≈ 8 dk").isBetween(470L, 481L);
    }

    @Test
    void shouldCountExpiredAndNotYetClosedPendingHolds_asAbandoned() throws Exception {
        long kullanici = yeniKullaniciId();
        Instant simdi = Instant.now();
        rezervasyonEkle(yeniSeansId(), kullanici, "EXPIRED", simdi.minus(Duration.ofMinutes(15)), null);
        rezervasyonEkle(yeniSeansId(), kullanici, "EXPIRED", simdi.minus(Duration.ofMinutes(5)), null);
        // Süresi geçmiş ama job henüz EXPIRED yapmamış: yine terk sayılır (doğruluk job'a bağlı değil)
        rezervasyonEkle(yeniSeansId(), kullanici, "PENDING_PAYMENT", simdi.minus(Duration.ofMinutes(1)), null);

        tut(kullanici, yeniSeansId(), "[1]").andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldAllowHold_whenOnlyTwoAbandonments() throws Exception {
        long kullanici = yeniKullaniciId();
        birakilmis(kullanici, Instant.now().minus(Duration.ofMinutes(3)));
        birakilmis(kullanici, Instant.now().minus(Duration.ofMinutes(1)));

        tut(kullanici, yeniSeansId(), "[1]").andExpect(status().isCreated());
    }

    @Test
    void shouldAllowHold_whenCooldownAfterLastAbandonmentPassed() throws Exception {
        long kullanici = yeniKullaniciId();
        Instant simdi = Instant.now();
        birakilmis(kullanici, simdi.minus(Duration.ofMinutes(25)));
        birakilmis(kullanici, simdi.minus(Duration.ofMinutes(20)));
        birakilmis(kullanici, simdi.minus(Duration.ofMinutes(11)));

        tut(kullanici, yeniSeansId(), "[1]").andExpect(status().isCreated());
    }

    @Test
    void shouldIgnoreAbandonmentsOutsideWindowAndPaidOrFailedReservations() throws Exception {
        long kullanici = yeniKullaniciId();
        Instant simdi = Instant.now();
        birakilmis(kullanici, simdi.minus(Duration.ofMinutes(31)));
        rezervasyonEkle(yeniSeansId(), kullanici, "CONFIRMED", simdi.minus(Duration.ofMinutes(5)), null);
        rezervasyonEkle(yeniSeansId(), kullanici, "FAILED", simdi.minus(Duration.ofMinutes(4)), null);
        birakilmis(kullanici, simdi.minus(Duration.ofMinutes(3)));
        birakilmis(kullanici, simdi.minus(Duration.ofMinutes(2)));

        tut(kullanici, yeniSeansId(), "[1]").andExpect(status().isCreated());
    }

    @Test
    void shouldBlockAfterThirdRealRelease_endToEnd() throws Exception {
        long kullanici = yeniKullaniciId();
        for (int i = 0; i < 3; i++) {
            String govde = tut(kullanici, yeniSeansId(), "[1]").andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            long id = ((Number) JsonPath.read(govde, "$.reservationId")).longValue();
            mockMvc.perform(delete("/api/v1/bookings/reservations/{id}", id).header("Authorization", yetki(kullanici)))
                    .andExpect(status().isNoContent());
        }

        tut(kullanici, yeniSeansId(), "[1]").andExpect(status().isTooManyRequests());
    }

    private void birakilmis(long kullanici, Instant birakilma) {
        // Bitiş bırakmadan sonra: süre dolmadan bırakılmış bir hold
        rezervasyonEkle(yeniSeansId(), kullanici, "RELEASED", birakilma.plus(Duration.ofMinutes(1)), birakilma);
    }
}
