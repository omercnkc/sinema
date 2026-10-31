package com.sinema.payment.odeme.service;

import com.sinema.payment.odeme.entity.Odeme;
import com.sinema.payment.odeme.repository.OdemeRepository;
import com.sinema.payment.saglayici.SorguSonucu;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ödeme kaydındaki durum değişikliklerinin transaction kısmı. Her metot ödeme satırını kilitleyerek okur: aynı anda
 * gelen iki callback (ya da callback ile mutabakat job'ı) aynı ödeme için sırayla karar verir. Sağlayıcı ve booking
 * çağrıları (HTTP) burada değil, çağıranda ve transaction dışında yapılır.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class OdemeSonucIslemi {

    static final String TUTAR_UYUSMAZ_KODU = "PROVIDER_RESULT_MISMATCH";

    private final OdemeRepository odemeRepository;

    /**
     * Sağlayıcı sonucunu uygular. Dönüş: bu çağrı sonucu uyguladıysa true (booking'e yalnızca o haber verir);
     * ödeme zaten sonuçlanmışsa ya da sonuç henüz yoksa (BEKLIYOR/BULUNAMADI) false.
     */
    @Transactional
    public boolean sonucuUygula(Long odemeId, SorguSonucu sonuc) {
        Odeme odeme = kilitliOku(odemeId);
        if (!odeme.sonucBekleniyorMu()) {
            return false;
        }
        switch (sonuc.durum()) {
            case BASARILI -> basariliSonucuUygula(odeme, sonuc);
            case BASARISIZ -> odeme.basarisizOldu(sonuc.hataKodu());
            case BEKLIYOR, BULUNAMADI -> {
                return false;
            }
        }
        return true;
    }

    /** booking onayı reddetti (süre doldu): para alındı, bilet yok → iade (§4.8). */
    @Transactional
    public void bookingReddetti(Long odemeId, String hataKodu) {
        kilitliOku(odemeId).iadeGerekli(hataKodu);
    }

    @Transactional
    public void senkronTamamlandi(Long odemeId) {
        kilitliOku(odemeId).bookingSenkronlandi();
    }

    @Transactional
    public void senkronErtelendi(Long odemeId, Instant sonrakiDeneme) {
        kilitliOku(odemeId).bookingSenkronuErtelendi(sonrakiDeneme);
    }

    /**
     * Tutar, para birimi, sepet ve konuşma kimliği kendi kaydımızla birebir aynı olmalı; değilse sonuç değiştirilmiş
     * ya da başka bir ödemeye ait demektir. Bilet verilmez; para alındığı için iade edilir.
     */
    private static void basariliSonucuUygula(Odeme odeme, SorguSonucu sonuc) {
        if (sonuc.eslesiyor(odeme.getTutar(), odeme.getParaBirimi(), String.valueOf(odeme.getRezervasyonId()),
                odeme.getKonusmaKimligi())) {
            odeme.basariliOldu(sonuc.saglayiciOdemeKimligi());
            return;
        }
        log.error("Sağlayıcı sonucu ödeme kaydıyla eşleşmiyor (ödeme {}); bilet verilmeyecek, iade edilecek",
                odeme.getId());
        odeme.iadeGerekli(TUTAR_UYUSMAZ_KODU);
        // Eşleşmeyen sonucun sağlayıcı kimliği yine de saklanır: iade bu kimlikle yapılır
        odeme.saglayiciOdemeKimliginiKaydet(sonuc.saglayiciOdemeKimligi());
    }

    private Odeme kilitliOku(Long odemeId) {
        return odemeRepository.findKilitliById(odemeId)
                .orElseThrow(() -> new IllegalStateException("Ödeme bulunamadı: " + odemeId));
    }
}
