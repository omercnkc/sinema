package com.sinema.booking.rezervasyon.service;

import com.sinema.booking.doluluk.service.DolulukOnbellegi;
import com.sinema.booking.koltuk.repository.KoltukKilitDeposu;
import com.sinema.booking.rezervasyon.dto.RezervasyonCevabi;
import com.sinema.booking.rezervasyon.entity.Rezervasyon;
import com.sinema.booking.rezervasyon.entity.RezervasyonDurumu;
import com.sinema.booking.rezervasyon.mapper.RezervasyonMapper;
import com.sinema.booking.rezervasyon.repository.RezervasyonRepository;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import com.sinema.common.sayfa.SayfaCevabi;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Kullanıcının kendi rezervasyonları: görüntüleme, listeleme ve ödeme öncesi hold'u bırakma (planlama §7.5, K40).
 * Her sorgu kullanıcı kimliğiyle sınırlıdır; başkasının kaydı 404 döner (var olduğu sızdırılmaz, §7.2).
 */
@Service
@RequiredArgsConstructor
public class RezervasyonServisi {

    public static final String REZERVASYON_YOK_KODU = "RESERVATION_NOT_FOUND";
    public static final String ONAYLI_KODU = "RESERVATION_ALREADY_CONFIRMED";
    public static final String ODEME_SURUYOR_KODU = "PAYMENT_IN_PROGRESS";
    public static final String AKTIF_DEGIL_KODU = "RESERVATION_NOT_ACTIVE";

    // En yeni işlem en üstte; aynı anda oluşturulanlarda sıra id ile kararlı kalır (sayfalar arası kayma olmaz)
    private static final Sort YENIDEN_ESKIYE = Sort.by(Sort.Order.desc("olusturulmaZamani"), Sort.Order.desc("id"));

    private final RezervasyonRepository rezervasyonRepository;
    private final KoltukKilitDeposu kilitDeposu;
    private final DolulukOnbellegi dolulukOnbellegi;
    private final RezervasyonMapper rezervasyonMapper;
    private final Clock saat;

    @Transactional(readOnly = true)
    public RezervasyonCevabi getir(Long kullaniciId, Long rezervasyonId) {
        return rezervasyonMapper.cevap(sahibininRezervasyonu(kullaniciId, rezervasyonId), simdi());
    }

    @Transactional(readOnly = true)
    public SayfaCevabi<RezervasyonCevabi> listele(Long kullaniciId, int sayfa, int boyut) {
        Instant simdi = simdi();
        return SayfaCevabi.from(rezervasyonRepository
                .findByKullaniciId(kullaniciId, PageRequest.of(sayfa, boyut, YENIDEN_ESKIYE))
                .map(rezervasyon -> rezervasyonMapper.cevap(rezervasyon, simdi)));
    }

    /**
     * Ödeme bekleyen hold'u bırakır: rezervasyon RELEASED, koltuklar hemen boşalır. Bırakma K29 terk sayacına dahildir
     * (hold-bırak sabotajı K29'un hedefi, K40). Zaten bırakılmışsa bir şey yapmaz (DELETE tekrarlanabilir).
     */
    @Transactional
    public void birak(Long kullaniciId, Long rezervasyonId) {
        Rezervasyon rezervasyon = sahibininRezervasyonu(kullaniciId, rezervasyonId);
        Instant simdi = simdi();
        if (rezervasyonRepository.birak(rezervasyonId, kullaniciId, simdi) == 0) {
            birakilamamaNedeniniBildir(rezervasyon, simdi);
            return;
        }
        kilitDeposu.tutmalariSil(rezervasyonId);
        commitSonrasiDolulukOnbelleginiSil(rezervasyon.getSeansId());
    }

    /**
     * Koşullu UPDATE 0 satır döndü: hangi kuralın engellediğini anlamlı bir kodla söyler. Karar, UPDATE'ten hemen
     * önce okunan duruma göre verilir; arada durum değiştiyse (ör. ödeme onaylandı) mesaj genel kalabilir, ama
     * yanlış bir değişiklik yapılmaz: değiştirme kararını koşullu UPDATE vermiştir.
     */
    private static void birakilamamaNedeniniBildir(Rezervasyon rezervasyon, Instant simdi) {
        RezervasyonDurumu durum = rezervasyon.getDurum();
        if (durum == RezervasyonDurumu.RELEASED) {
            return;
        }
        if (durum == RezervasyonDurumu.CONFIRMED) {
            throw new CakismaIstisnasi(ONAYLI_KODU, "Satın alınmış bilet iptal edilemez.");
        }
        if (durum == RezervasyonDurumu.PENDING_PAYMENT && rezervasyon.isOdemePenceresiUzatildi()
                && rezervasyon.getBitis().isAfter(simdi)) {
            throw new CakismaIstisnasi(ODEME_SURUYOR_KODU, "Ödeme işlemi sürüyor; şu an bırakılamaz.");
        }
        throw new CakismaIstisnasi(AKTIF_DEGIL_KODU, "Bu işlemin süresi dolmuş ya da kapanmış.");
    }

    /**
     * Önbellek commit'ten sonra silinir: commit'ten önce silinseydi, arada gelen bir doluluk isteği henüz commit
     * edilmemiş eski durumu okuyup 2 sn boyunca önbellekte tutabilirdi.
     */
    private void commitSonrasiDolulukOnbelleginiSil(Long seansId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dolulukOnbellegi.sil(seansId);
            }
        });
    }

    private Rezervasyon sahibininRezervasyonu(Long kullaniciId, Long rezervasyonId) {
        return rezervasyonRepository.findByIdAndKullaniciId(rezervasyonId, kullaniciId)
                .orElseThrow(() -> new KaynakBulunamadiIstisnasi(REZERVASYON_YOK_KODU, "Rezervasyon bulunamadı."));
    }

    // DB DATETIME(3) saklar; cevaplardaki zamanlar okunan değerlerle aynı hassasiyette olsun
    private Instant simdi() {
        return saat.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}
