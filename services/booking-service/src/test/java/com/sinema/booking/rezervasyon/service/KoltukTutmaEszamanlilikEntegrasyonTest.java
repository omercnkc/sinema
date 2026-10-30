package com.sinema.booking.rezervasyon.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sinema.booking.rezervasyon.RezervasyonEntegrasyonTesti;
import com.sinema.booking.rezervasyon.dto.KoltukTutmaIstegi;
import com.sinema.common.hata.SinemaIstisnasi;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Planlama §4.10 testleri 1-4: K9 (aynı koltuk iki kişiye verilmez) gerçek MySQL üzerinde, gerçek eşzamanlı
 * işlemlerle. Her işlem ya başarılı olur ya da beklenen iş hatasını alır; başka bir hata (deadlock, 500) testi bozar.
 */
class KoltukTutmaEszamanlilikEntegrasyonTest extends RezervasyonEntegrasyonTesti {

    private static final int ESZAMANLI_ISTEK = 20;
    private static final String BASARILI = "BASARILI";

    @Autowired
    private KoltukTutmaServisi koltukTutmaServisi;

    @Autowired
    private OdemeAkisiServisi odemeAkisiServisi;

    @Autowired
    private RezervasyonServisi rezervasyonServisi;

    @Test
    void shouldGiveSeatToExactlyOneUser_whenManyUsersHoldSameSeatAtOnce() throws Exception {
        long seansId = yeniSeansId();

        List<String> sonuclar = esZamanliCalistir(ESZAMANLI_ISTEK, sira -> tut(1000L + sira, seansId, 1L));

        assertThat(sonuclar).filteredOn(BASARILI::equals).hasSize(1);
        assertThat(sonuclar).filteredOn(sonuc -> !sonuc.equals(BASARILI))
                .containsOnly(KoltukTutmaServisi.KOLTUK_DOLU_KODU);
        assertThat(koltukSahibi(seansId, 1L)).isNotNull();
    }

    @Test
    void shouldLetExactlyOneUserTakeOverExpiredHold_whenManyTryAtOnce() throws Exception {
        long seansId = yeniSeansId();
        long eskiRezervasyon = suresiDolmusTutmaEkle(seansId, 1L, 999L);

        List<String> sonuclar = esZamanliCalistir(ESZAMANLI_ISTEK, sira -> tut(2000L + sira, seansId, 1L));

        assertThat(sonuclar).filteredOn(BASARILI::equals).hasSize(1);
        assertThat(sonuclar).filteredOn(sonuc -> !sonuc.equals(BASARILI))
                .containsOnly(KoltukTutmaServisi.KOLTUK_DOLU_KODU);
        assertThat(koltukSahibi(seansId, 1L)).isNotEqualTo(eskiRezervasyon);
    }

    @Test
    void shouldTakeNoSeatAtAll_whenOneOfRequestedSeatsIsTaken() {
        long seansId = yeniSeansId();
        tut(3000L, seansId, 3L);

        String sonuc = tut(3001L, seansId, 1L, 2L, 3L, 4L);

        assertThat(sonuc).isEqualTo(KoltukTutmaServisi.KOLTUK_DOLU_KODU);
        assertThat(List.of(1L, 2L, 4L)).allSatisfy(hucre -> assertThat(koltukSahibi(seansId, hucre)).isNull());
        assertThat(seansinRezervasyonSayisi(seansId)).as("yalnızca ilk kullanıcının işlemi").isEqualTo(1);
    }

    @Test
    void shouldFinishWithoutDeadlock_whenUsersSelectSameSeatsInOppositeOrder() throws Exception {
        for (int tur = 0; tur < 10; tur++) {
            long seansId = yeniSeansId();

            List<String> sonuclar = esZamanliCalistir(ESZAMANLI_ISTEK, sira -> sira % 2 == 0
                    ? tut(4000L + sira, seansId, 1L, 2L, 3L)
                    : tut(4000L + sira, seansId, 3L, 2L, 1L));

            assertThat(sonuclar).filteredOn(BASARILI::equals).hasSize(1);
            assertThat(sonuclar).filteredOn(sonuc -> !sonuc.equals(BASARILI))
                    .containsOnly(KoltukTutmaServisi.KOLTUK_DOLU_KODU);
            Long sahip = koltukSahibi(seansId, 1L);
            assertThat(koltukSahibi(seansId, 2L)).isEqualTo(sahip);
            assertThat(koltukSahibi(seansId, 3L)).isEqualTo(sahip);
        }
    }

    @Test
    void shouldAllowOnlyOneActiveReservation_whenSameUserHoldsTwiceAtOnce() throws Exception {
        long seansId = yeniSeansId();

        List<String> sonuclar = esZamanliCalistir(ESZAMANLI_ISTEK, sira -> tut(5000L, seansId, 100L + sira));

        assertThat(sonuclar).filteredOn(BASARILI::equals).hasSize(1);
        assertThat(sonuclar).filteredOn(sonuc -> !sonuc.equals(BASARILI))
                .containsOnly(KoltukTutmaServisi.AKTIF_REZERVASYON_KODU);
        assertThat(seansinRezervasyonSayisi(seansId)).isEqualTo(1);
    }

    @Test
    void shouldApplyExactlyOneOfConfirmAndRelease_whenTheyRace() throws Exception {
        for (int tur = 0; tur < 10; tur++) {
            long kullanici = yeniKullaniciId();
            long seansId = yeniSeansId();
            long rezervasyonId = tutVeIdAl(kullanici, seansId, "[1, 2]");

            List<String> sonuclar = esZamanliCalistir(2, sira -> sira == 0
                    ? isKoduIle(() -> odemeAkisiServisi.onayla(rezervasyonId, 1L))
                    : isKoduIle(() -> rezervasyonServisi.birak(kullanici, rezervasyonId)));

            assertThat(sonuclar).filteredOn(BASARILI::equals).hasSize(1);
            String durum = rezervasyonDurumu(rezervasyonId);
            assertThat(durum).isIn("CONFIRMED", "RELEASED");
            List<String> koltuklar = jdbcTemplate.queryForList(
                    "SELECT status FROM seat_bookings WHERE reservation_id = ?", String.class, rezervasyonId);
            if (durum.equals("CONFIRMED")) {
                assertThat(koltuklar).containsExactly("SOLD", "SOLD");
            } else {
                assertThat(koltuklar).isEmpty();
            }
        }
    }

    private static String isKoduIle(Runnable is) {
        try {
            is.run();
            return BASARILI;
        } catch (SinemaIstisnasi e) {
            return e.getKod();
        }
    }

    /** Başarıda BASARILI, beklenen iş hatasında hata kodu döner; beklenmeyen hata testi düşürür. */
    private String tut(long kullaniciId, long seansId, Long... hucreler) {
        try {
            koltukTutmaServisi.tut(kullaniciId, new KoltukTutmaIstegi(seansId, List.of(hucreler)));
            return BASARILI;
        } catch (SinemaIstisnasi e) {
            return e.getKod();
        }
    }

    /** Tüm iş parçacıkları aynı anda başlar (kapı), böylece istekler gerçekten çakışır. */
    private static List<String> esZamanliCalistir(int adet, SiraliGorev gorev) throws Exception {
        ExecutorService havuz = Executors.newFixedThreadPool(adet);
        CountDownLatch kapi = new CountDownLatch(1);
        try {
            List<Future<String>> gelecekler = new ArrayList<>();
            for (int sira = 0; sira < adet; sira++) {
                int buSira = sira;
                Callable<String> is = () -> {
                    kapi.await();
                    return gorev.calistir(buSira);
                };
                gelecekler.add(havuz.submit(is));
            }
            kapi.countDown();
            List<String> sonuclar = new ArrayList<>();
            for (Future<String> gelecek : gelecekler) {
                sonuclar.add(gelecek.get(30, TimeUnit.SECONDS));
            }
            return sonuclar;
        } finally {
            havuz.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface SiraliGorev {
        String calistir(int sira);
    }
}
