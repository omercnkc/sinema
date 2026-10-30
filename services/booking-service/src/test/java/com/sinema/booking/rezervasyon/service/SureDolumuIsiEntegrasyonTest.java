package com.sinema.booking.rezervasyon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sinema.booking.rezervasyon.RezervasyonEntegrasyonTesti;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Süre dolumu job'ı (planlama §4.6, K43): yalnızca süresi dolanlara dokunur, SOLD satırlar kalır. */
class SureDolumuIsiEntegrasyonTest extends RezervasyonEntegrasyonTesti {

    @Autowired
    private SureDolumuIsi sureDolumuIsi;

    @Autowired
    private OdemeAkisiServisi odemeAkisiServisi;

    @Test
    void shouldExpireReservationAndDeleteHeldSeats_whenHoldExpired() {
        long seansId = yeniSeansId();
        long rezervasyonId = suresiDolmusTutmaEkle(seansId, 1, yeniKullaniciId());

        sureDolumuIsi.calistir();

        assertThat(rezervasyonDurumu(rezervasyonId)).isEqualTo("EXPIRED");
        assertThat(koltukSahibi(seansId, 1)).isNull();
    }

    @Test
    void shouldLeaveActiveHoldsAndSoldSeatsUntouched() throws Exception {
        long seansId = yeniSeansId();
        long aktif = tutVeIdAl(yeniKullaniciId(), seansId, "[1]");
        long satilan = tutVeIdAl(yeniKullaniciId(), seansId, "[2]");
        odemeAkisiServisi.onayla(satilan, 1L);
        // Satılmış rezervasyonun bitişi geçmiş olsa da SOLD satır silinmez (K6)
        jdbcTemplate.update("UPDATE reservations SET expires_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(60)), satilan);

        sureDolumuIsi.calistir();

        assertThat(rezervasyonDurumu(aktif)).isEqualTo("PENDING_PAYMENT");
        assertThat(koltukSahibi(seansId, 1)).isEqualTo(aktif);
        assertThat(rezervasyonDurumu(satilan)).isEqualTo("CONFIRMED");
        assertThat(koltukSahibi(seansId, 2)).isEqualTo(satilan);
    }

    @Test
    void shouldStillCountAsAbandonmentForK29_afterJobClosesReservation() throws Exception {
        long kullanici = yeniKullaniciId();
        for (int i = 0; i < 3; i++) {
            suresiDolmusTutmaEkle(yeniSeansId(), 1, kullanici);
        }

        sureDolumuIsi.calistir();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE user_id = ? AND status = 'EXPIRED'", Integer.class,
                kullanici)).isEqualTo(3);
        tut(kullanici, yeniSeansId(), "[1]").andExpect(status().isTooManyRequests());
    }
}
