package com.sinema.booking.rezervasyon.service;

import com.sinema.booking.koltuk.repository.KoltukKilitDeposu;
import com.sinema.booking.koltuk.repository.KoltukKilitDeposu.KoltukTutma;
import com.sinema.booking.rezervasyon.entity.Rezervasyon;
import com.sinema.booking.rezervasyon.entity.RezervasyonKoltugu;
import com.sinema.booking.rezervasyon.repository.RezervasyonRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Koltuk tutmanın veritabanı kısmı, tek transaction (planlama §4.3 adım 2-3). Catalog çağrıları bunun dışında
 * yapılır (KoltukTutmaServisi): HTTP beklerken DB transaction'ı ve satır kilitleri açık kalmaz.
 *
 * READ COMMITTED: MySQL'in varsayılanı REPEATABLE READ'de, olmayan satırı arayan UPDATE "gap lock" alır ve aynı
 * boşluğa INSERT yapan iki işlem birbirini kilitler (deadlock). READ COMMITTED'da gap lock yok; K9'u yine
 * UNIQUE kısıtı ve satır kilitleri garanti eder.
 */
@Component
@RequiredArgsConstructor
class KoltukTutmaIslemi {

    private final RezervasyonRepository rezervasyonRepository;
    private final KoltukKilitDeposu kilitDeposu;

    /**
     * Koltuklar hücre id'sine göre sıralı gelmeli: tüm işlemler kilitleri aynı sırayla alır, ters sırada seçen iki
     * kullanıcı birbirini beklemez (deadlock yok, §4.3). Bir koltuk bile alınamazsa her şey geri alınır.
     *
     * @throws KoltukAlinamadiIstisnasi bir koltuk dolu
     * @throws org.springframework.dao.DataIntegrityViolationException kullanıcının bu seansta aktif işlemi var
     *         (active_pending_key UNIQUE, eşzamanlı iki istekte ikincisi)
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Rezervasyon olustur(Long kullaniciId, Rezervasyon.SeansKopyasi seans, List<RezervasyonKoltugu> koltuklar,
            Instant bitis, Instant simdi) {
        rezervasyonRepository.suresiDolanBekleyeniKapat(kullaniciId, seans.seansId(), simdi);
        // Hemen flush: koltuk satırları reservation_id FK'si için rezervasyonun id'sine ihtiyaç duyar
        Rezervasyon rezervasyon = rezervasyonRepository.saveAndFlush(
                Rezervasyon.beklemedeOlustur(kullaniciId, seans, koltuklar, bitis));
        for (RezervasyonKoltugu koltuk : koltuklar) {
            KoltukTutma tutma = new KoltukTutma(seans.seansId(), koltuk.getHucreId(), kullaniciId,
                    rezervasyon.getId(), bitis, simdi);
            if (!kilitDeposu.tut(tutma)) {
                throw new KoltukAlinamadiIstisnasi(koltuk.getHucreId());
            }
        }
        return rezervasyon;
    }
}
