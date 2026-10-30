package com.sinema.booking.rezervasyon.service;

import com.sinema.booking.config.RezervasyonAyarlari;
import com.sinema.booking.doluluk.service.DolulukTemizleyici;
import com.sinema.booking.koltuk.repository.KoltukKilitDeposu;
import com.sinema.booking.rezervasyon.dto.OdemeBilgisi;
import com.sinema.booking.rezervasyon.dto.OnayCevabi;
import com.sinema.booking.rezervasyon.entity.Rezervasyon;
import com.sinema.booking.rezervasyon.entity.RezervasyonDurumu;
import com.sinema.booking.rezervasyon.repository.RezervasyonRepository;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * payment-service'in çağırdığı rezervasyon işlemleri (planlama §4.5, §4.8, §7.5, K32). HELD → SOLD geçişini yalnızca
 * booking yapar; payment yalnızca "onayla" der (§3.4-4).
 *
 * Her yazma işlemi rezervasyon satırını kilitleyerek okur: aynı rezervasyonda onay, ret, uzatma ve kullanıcının
 * bırakması sırayla uygulanır. Hepsi tekrarlanabilir (idempotent): payment ağ hatasında aynı çağrıyı tekrar
 * gönderebilir, sonuç değişmez.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OdemeAkisiServisi {

    private final RezervasyonRepository rezervasyonRepository;
    private final KoltukKilitDeposu kilitDeposu;
    private final BiletKoduUretici biletKoduUretici;
    private final DolulukTemizleyici dolulukTemizleyici;
    private final RezervasyonAyarlari ayarlar;
    private final Clock saat;

    @Transactional(readOnly = true)
    public OdemeBilgisi bilgi(Long rezervasyonId) {
        Rezervasyon rezervasyon = rezervasyonRepository.findById(rezervasyonId).orElseThrow(this::rezervasyonYok);
        return odemeBilgisi(rezervasyon, simdi());
    }

    /**
     * K32: ödeme başlatılınca hold bir kez uzar (satış kapanışını aşmadan), koltuk tutmaları da aynı bitişe çekilir.
     * Zaten uzatılmışsa mevcut durum döner. Süresi dolmuş ya da kapanmış işlem uzatılamaz (409).
     */
    @Transactional
    public OdemeBilgisi odemePenceresiniUzat(Long rezervasyonId) {
        Instant simdi = simdi();
        Rezervasyon rezervasyon = kilitliOku(rezervasyonId);
        odenebilirOlmali(rezervasyon, simdi);
        if (!rezervasyon.isOdemePenceresiUzatildi()) {
            rezervasyon.odemePenceresiniUzat(ayarlar.odemePenceresi());
            kilitDeposu.tutmaBitisiniGuncelle(rezervasyonId, rezervasyon.getBitis(), simdi);
        }
        return odemeBilgisi(rezervasyon, simdi);
    }

    /**
     * §4.5: ödeme başarılı → tüm koltuklar tek transaction'da SOLD, rezervasyon CONFIRMED + bilet kodu (K30).
     * Süre dolduysa (P2 penceresi dahil) onay yok, 409: payment ödemeyi iade eder (§4.8; kullanıcı iptali değil, K6).
     */
    @Transactional
    public OnayCevabi onayla(Long rezervasyonId, Long odemeId) {
        Instant simdi = simdi();
        Rezervasyon rezervasyon = kilitliOku(rezervasyonId);
        if (rezervasyon.getDurum() == RezervasyonDurumu.CONFIRMED) {
            return onayCevabi(rezervasyon);
        }
        odenebilirOlmali(rezervasyon, simdi);
        int satilan = kilitDeposu.sat(rezervasyonId, simdi);
        if (satilan != rezervasyon.getKoltuklar().size()) {
            // Koltuk tutmaları rezervasyonla aynı anda dolar; olmaması gerekir. Hepsi ya da hiçbiri: geri alınır.
            log.error("Onayda koltuk sayısı tutmadı (rezervasyon {}, ödeme {}): {}/{}", rezervasyonId, odemeId,
                    satilan, rezervasyon.getKoltuklar().size());
            throw aktifDegil();
        }
        rezervasyon.onayla(biletKoduUretici.uret(), simdi);
        dolulukTemizleyici.degisti(rezervasyon.getSeansId());
        log.info("Rezervasyon onaylandı (rezervasyon {}, ödeme {})", rezervasyonId, odemeId);
        return onayCevabi(rezervasyon);
    }

    /** Ödeme reddedildi: rezervasyon FAILED, koltuklar hemen boşalır. Zaten kapanmışsa bir şey yapmaz. */
    @Transactional
    public void basarisiz(Long rezervasyonId) {
        Rezervasyon rezervasyon = kilitliOku(rezervasyonId);
        if (rezervasyon.getDurum() == RezervasyonDurumu.CONFIRMED) {
            throw onayli();
        }
        if (rezervasyon.getDurum() != RezervasyonDurumu.PENDING_PAYMENT) {
            return;
        }
        rezervasyon.basarisizYap();
        kilitDeposu.tutmalariSil(rezervasyonId);
        dolulukTemizleyici.degisti(rezervasyon.getSeansId());
    }

    private void odenebilirOlmali(Rezervasyon rezervasyon, Instant simdi) {
        if (rezervasyon.getDurum() == RezervasyonDurumu.CONFIRMED) {
            throw onayli();
        }
        if (!rezervasyon.odenebilirMi(simdi)) {
            throw aktifDegil();
        }
    }

    private Rezervasyon kilitliOku(Long rezervasyonId) {
        return rezervasyonRepository.findKilitliById(rezervasyonId).orElseThrow(this::rezervasyonYok);
    }

    private KaynakBulunamadiIstisnasi rezervasyonYok() {
        return new KaynakBulunamadiIstisnasi(RezervasyonServisi.REZERVASYON_YOK_KODU, "Rezervasyon bulunamadı.");
    }

    private static CakismaIstisnasi onayli() {
        return new CakismaIstisnasi(RezervasyonServisi.ONAYLI_KODU, "Rezervasyon zaten onaylanmış.");
    }

    private static CakismaIstisnasi aktifDegil() {
        return new CakismaIstisnasi(RezervasyonServisi.AKTIF_DEGIL_KODU, "Rezervasyonun süresi dolmuş ya da kapanmış.");
    }

    private static OdemeBilgisi odemeBilgisi(Rezervasyon rezervasyon, Instant simdi) {
        return new OdemeBilgisi(rezervasyon.getId(), rezervasyon.getKullaniciId(), rezervasyon.getDurum(),
                rezervasyon.getToplamTutar(), rezervasyon.getBitis(), rezervasyon.isOdemePenceresiUzatildi(), simdi);
    }

    private static OnayCevabi onayCevabi(Rezervasyon rezervasyon) {
        return new OnayCevabi(rezervasyon.getId(), rezervasyon.getDurum(), rezervasyon.getBiletKodu());
    }

    private Instant simdi() {
        return saat.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}
