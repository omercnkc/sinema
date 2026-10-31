package com.sinema.payment.odeme.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sinema.payment.odeme.entity.BookingSenkronDurumu;
import com.sinema.payment.odeme.entity.Odeme;
import com.sinema.payment.odeme.entity.OdemeDurumu;
import com.sinema.payment.odeme.entity.OdemeSaglayicisi;
import com.sinema.payment.odeme.entity.OlayKaynagi;
import com.sinema.payment.odeme.entity.SaglayiciKaydi;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** V2 şeması ile entity eşlemesi gerçek MySQL'de: kısıtlar ve JSON kolonu. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MysqlTestYapilandirmasi.class)
class OdemeRepositoryEntegrasyonTest {

    private static final AtomicLong REZERVASYON_SAYACI = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    @Autowired
    private OdemeRepository odemeRepository;

    @Autowired
    private SaglayiciKaydiRepository kayitRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldStartAsInitiatedInTry_whenPaymentCreated() {
        long rezervasyonId = REZERVASYON_SAYACI.incrementAndGet();

        Odeme odeme = odemeRepository.saveAndFlush(yeniOdeme(rezervasyonId));

        Odeme okunan = odemeRepository.findByRezervasyonId(rezervasyonId).orElseThrow();
        assertThat(okunan.getId()).isEqualTo(odeme.getId());
        assertThat(okunan.getDurum()).isEqualTo(OdemeDurumu.INITIATED);
        assertThat(okunan.getBookingSenkronDurumu()).isEqualTo(BookingSenkronDurumu.NOT_NEEDED);
        assertThat(okunan.getParaBirimi()).isEqualTo("TRY");
        assertThat(okunan.getTutar()).isEqualByComparingTo("300.00");
    }

    @Test
    void shouldRejectSecondPayment_whenSameReservation() {
        long rezervasyonId = REZERVASYON_SAYACI.incrementAndGet();
        odemeRepository.saveAndFlush(yeniOdeme(rezervasyonId));

        assertThatThrownBy(() -> odemeRepository.saveAndFlush(yeniOdeme(rezervasyonId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldStoreRawPayloadAsJson_andAllowUnknownPayment() {
        Instant simdi = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        SaglayiciKaydi kayit = kayitRepository.saveAndFlush(SaglayiciKaydi.olustur(null, "bilinmeyen-token",
                OlayKaynagi.CALLBACK, null, "{\"token\":\"bilinmeyen-token\"}", simdi));

        String tur = jdbcTemplate.queryForObject(
                "SELECT JSON_TYPE(raw_payload) FROM payment_callback_logs WHERE id = ?", String.class, kayit.getId());
        assertThat(tur).isEqualTo("OBJECT");
    }

    private static Odeme yeniOdeme(long rezervasyonId) {
        return Odeme.olustur(rezervasyonId, 42L, OdemeSaglayicisi.MOCK, new BigDecimal("300.00"),
                UUID.randomUUID().toString());
    }
}
