package com.sinema.payment.odeme.service;

import com.sinema.common.hata.SinemaIstisnasi;
import com.sinema.common.servisarasi.BagimlilikHatalari;
import com.sinema.payment.config.OdemeAyarlari;
import com.sinema.payment.odeme.entity.BookingSenkronDurumu;
import com.sinema.payment.odeme.entity.Odeme;
import com.sinema.payment.odeme.entity.OdemeDurumu;
import com.sinema.payment.odeme.repository.OdemeRepository;
import com.sinema.payment.rezervasyon.istemci.OnayIstegi;
import com.sinema.payment.rezervasyon.istemci.RezervasyonIstemcisi;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Ödeme sonucunu booking'e iletir (§4.5, §4.8): başarılıysa confirm (koltuklar SOLD), başarısızsa fail (koltuklar
 * boşalır). booking iki çağrıyı da tekrarlanabilir işlediği için yeniden denemek güvenlidir. Callback ve mutabakat
 * job'ı aynı sınıfı kullanır.
 *
 * - booking'e ulaşılamadı (503): senkron PENDING kalır, belirli bir süre sonra yeniden denenir.
 * - booking onayı reddetti (409, ör. hold süresi doldu): para alındı ama bilet yok → REFUND_REQUIRED, iade edilir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSenkronlayici {

    private final OdemeRepository odemeRepository;
    private final RezervasyonIstemcisi rezervasyonIstemcisi;
    private final OdemeSonucIslemi sonucIslemi;
    private final OdemeAyarlari ayarlar;
    private final Clock saat;

    public void senkronla(Long odemeId) {
        Odeme odeme = odemeRepository.findById(odemeId).orElseThrow();
        if (odeme.getBookingSenkronDurumu() != BookingSenkronDurumu.PENDING) {
            return;
        }
        try {
            ilet(odeme);
            sonucIslemi.senkronTamamlandi(odemeId);
        } catch (SinemaIstisnasi e) {
            hatayiIsle(odeme, e);
        }
    }

    private void ilet(Odeme odeme) {
        if (odeme.getDurum() == OdemeDurumu.SUCCEEDED) {
            rezervasyonIstemcisi.onayla(odeme.getRezervasyonId(), new OnayIstegi(odeme.getId()));
        } else {
            rezervasyonIstemcisi.basarisiz(odeme.getRezervasyonId());
        }
    }

    private void hatayiIsle(Odeme odeme, SinemaIstisnasi hata) {
        if (!BagimlilikHatalari.isSonucuMu(hata)) {
            log.warn("Ödeme sonucu booking'e iletilemedi (ödeme {}); yeniden denenecek", odeme.getId());
            sonucIslemi.senkronErtelendi(odeme.getId(), saat.instant().plus(ayarlar.bookingSenkronBeklemesi()));
            return;
        }
        if (odeme.getDurum() == OdemeDurumu.SUCCEEDED) {
            log.warn("booking onayı reddetti (ödeme {}, kod {}); ödeme iade edilecek", odeme.getId(), hata.getKod());
            sonucIslemi.bookingReddetti(odeme.getId(), hata.getKod());
            return;
        }
        // Başarısız ödemenin reddi: rezervasyon zaten kapanmış (süresi dolmuş ya da bırakılmış), yapılacak bir şey yok
        sonucIslemi.senkronTamamlandi(odeme.getId());
    }
}
