package com.sinema.payment.odeme.service;

import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.payment.config.MutabakatAyarlari;
import com.sinema.payment.odeme.entity.Odeme;
import com.sinema.payment.odeme.repository.OdemeRepository;
import com.sinema.payment.odeme.repository.SaglayiciKaydiRepository;
import com.sinema.payment.saglayici.IadeSonucu;
import com.sinema.payment.saglayici.OdemeSaglayiciAdaptoru;
import com.sinema.payment.saglayici.SorguDurumu;
import com.sinema.payment.saglayici.SorguSonucu;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

/**
 * Mutabakat (planlama §4.8): callback'e güvenilmediği ve tarayıcı yolda kaybolabildiği için ödemelerin son durumu
 * düzenli olarak sağlayıcı ve booking ile eşitlenir. Her iş bağımsızdır ve tekrarlanabilir; bir ödemedeki hata
 * diğerlerini durdurmaz. Sağlayıcı/booking çağrıları DB transaction'ı dışında, durum değişiklikleri kilitli
 * transaction'da (OdemeSonucIslemi) yapılır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OdemeMutabakatServisi {

    private final OdemeRepository odemeRepository;
    private final SaglayiciKaydiRepository kayitRepository;
    private final OdemeSaglayiciAdaptoru saglayici;
    private final OdemeSonucIslemi sonucIslemi;
    private final BookingSenkronlayici bookingSenkronlayici;
    private final SaglayiciKayitcisi kayitci;
    private final MutabakatAyarlari ayarlar;
    private final Clock saat;

    public void calistir() {
        senkronlariYenidenDene();
        bekleyenOdemeleriKapat();
        iadeleriYap();
    }

    /** booking'e iletilememiş onay/ret (booking o an çalışmıyordu). */
    public void senkronlariYenidenDene() {
        Instant simdi = saat.instant();
        odemeRepository.senkronuBekleyenler(simdi, simdi.minus(ayarlar.sakinlikSuresi()), limit())
                .forEach(guvenle(bookingSenkronlayici::senkronla));
    }

    /** Callback'i hiç gelmemiş ödemeler: kullanıcı ödeyip tarayıcıyı kapattıysa ya da sayfayı terk ettiyse. */
    public void bekleyenOdemeleriKapat() {
        odemeRepository.sonucuBeklenenler(saglayici.tur(), saat.instant().minus(ayarlar.sakinlikSuresi()), limit())
                .forEach(guvenle(this::bekleyeniKapat));
    }

    /** Para alınmış ama bilet verilememiş ödemeler (§4.8; kullanıcı iptali değil, K6). */
    public void iadeleriYap() {
        odemeRepository.iadeyiBekleyenler(saglayici.tur(), saat.instant(), limit()).forEach(guvenle(this::iadeEt));
    }

    /** Saklama süresi geçmiş ham kayıtlar (K43), partiler halinde. */
    public int eskiKayitlariSil() {
        Instant esik = saat.instant().minus(ayarlar.kayitSaklama());
        int toplam = 0;
        int silinen;
        do {
            silinen = kayitRepository.eskileriSil(esik, ayarlar.partiBoyutu());
            toplam += silinen;
        } while (silinen == ayarlar.partiBoyutu());
        return toplam;
    }

    /**
     * Sonuç varsa callback'teki aynı yoldan uygulanır. Token süresi dolduğu halde ödeme yoksa (BEKLIYOR ya da
     * BULUNAMADI) kullanıcı sayfayı terk etmiştir. Sağlayıcıya ulaşılamazsa sonraki çalışmada yeniden sorulur.
     */
    private void bekleyeniKapat(Long odemeId) {
        Odeme odeme = odemeRepository.findById(odemeId).orElseThrow();
        SorguSonucu sonuc;
        try {
            sonuc = saglayici.sonucuSorgula(odeme.getSaglayiciTokeni(), odeme.getKonusmaKimligi());
        } catch (BagimlilikKullanilamazIstisnasi e) {
            return;
        }
        kayitci.sorgu(odemeId, odeme.getSaglayiciTokeni(), sonuc);
        if (sonucIslemi.sonucuUygula(odemeId, sonuc)) {
            bookingSenkronlayici.senkronla(odemeId);
            return;
        }
        boolean sonucYok = sonuc.durum() == SorguDurumu.BEKLIYOR || sonuc.durum() == SorguDurumu.BULUNAMADI;
        if (sonucYok && !odeme.getTokenBitisi().isAfter(saat.instant())) {
            log.info("Ödeme sayfası terk edildi (ödeme {})", odemeId);
            sonucIslemi.terkEdildi(odemeId);
        }
    }

    private void iadeEt(Long odemeId) {
        Odeme odeme = odemeRepository.findById(odemeId).orElseThrow();
        IadeSonucu sonuc;
        try {
            sonuc = saglayici.iadeEt(odeme.getSaglayiciOdemeKimligi(), odeme.getTutar(), odeme.getKonusmaKimligi());
        } catch (BagimlilikKullanilamazIstisnasi e) {
            sonuc = IadeSonucu.reddedildi(e.getKod());
        }
        if (sonuc.basarili()) {
            log.info("Ödeme iade edildi (ödeme {}, rezervasyon {})", odemeId, odeme.getRezervasyonId());
            sonucIslemi.iadeEdildi(odemeId);
            return;
        }
        log.warn("İade yapılamadı (ödeme {}, kod {}); yeniden denenecek", odemeId, sonuc.hataKodu());
        sonucIslemi.iadeErtelendi(odemeId, sonuc.hataKodu(), saat.instant().plus(ayarlar.iadeBeklemesi()));
    }

    /** Bir ödemedeki beklenmeyen hata diğerlerini durdurmasın; sonraki çalışmada yeniden denenir. */
    private Consumer<Long> guvenle(Consumer<Long> is) {
        return odemeId -> {
            try {
                is.accept(odemeId);
            } catch (RuntimeException e) {
                log.error("Mutabakat işi başarısız (ödeme {})", odemeId, e);
            }
        };
    }

    private Limit limit() {
        return Limit.of(ayarlar.partiBoyutu());
    }
}
