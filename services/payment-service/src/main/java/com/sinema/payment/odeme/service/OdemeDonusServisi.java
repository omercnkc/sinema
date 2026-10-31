package com.sinema.payment.odeme.service;

import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.payment.config.OdemeAyarlari;
import com.sinema.payment.odeme.entity.Odeme;
import com.sinema.payment.odeme.entity.OlayKaynagi;
import com.sinema.payment.odeme.entity.SaglayiciKaydi;
import com.sinema.payment.odeme.repository.OdemeRepository;
import com.sinema.payment.odeme.repository.SaglayiciKaydiRepository;
import com.sinema.payment.saglayici.OdemeSaglayiciAdaptoru;
import com.sinema.payment.saglayici.SorguSonucu;
import java.net.URI;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Sağlayıcının ödeme sonrası tarayıcı yönlendirmesi (callback, K39). Callback'in kendisine güvenilmez: kim olursa olsun
 * bu adrese token POST edebilir. Sonuç token ile sağlayıcıdan sorgulanır ve kendi kaydımızla karşılaştırılır; sahte
 * callback bilet üretemez. Her olay ham haliyle denetim kaydına yazılır (bilinmeyen token dahil).
 *
 * Tekrarlanabilir: aynı callback ikinci kez gelirse sonuç yeniden uygulanmaz; yalnızca booking'e iletilememiş bir sonuç
 * varsa iletme yeniden denenir. Kullanıcı her durumda ön yüzün sonuç sayfasına yönlendirilir; sayfa durumu
 * rezervasyondan okur (§8.6).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OdemeDonusServisi {

    static final String BILINMEYEN_ODEME = "PAYMENT_NOT_FOUND";

    private final OdemeRepository odemeRepository;
    private final SaglayiciKaydiRepository kayitRepository;
    private final OdemeSaglayiciAdaptoru saglayici;
    private final OdemeSonucIslemi sonucIslemi;
    private final BookingSenkronlayici bookingSenkronlayici;
    private final OdemeAyarlari ayarlar;
    private final Clock saat;

    /** Dönüş: tarayıcının yönlendirileceği ön yüz adresi. */
    public URI isle(String token) {
        Optional<Odeme> bulunan = odemeRepository.findBySaglayiciTokeni(token);
        kaydet(bulunan.map(Odeme::getId).orElse(null), token, OlayKaynagi.CALLBACK, null, callbackGovdesi(token));
        if (bulunan.isEmpty()) {
            log.warn("Bilinmeyen token ile ödeme dönüşü geldi");
            return sonucSayfasi().queryParam("error", BILINMEYEN_ODEME).build().toUri();
        }
        Odeme odeme = bulunan.get();
        // Sonucu bu çağrı uyguladıysa ya da sonuç önceden uygulanmışsa (tekrar callback) booking'e iletilir.
        // Aynı anda gelen ikinci callback sonucu uygulayamaz ve iletmez: booking'e tek onay gider.
        boolean iletilmeli = !odeme.sonucBekleniyorMu() || sonucuSorgulaVeUygula(odeme);
        if (iletilmeli) {
            bookingSenkronlayici.senkronla(odeme.getId());
        }
        return sonucSayfasi().queryParam("reservationId", odeme.getRezervasyonId()).build().toUri();
    }

    /**
     * Dönüş: sonucu bu çağrı uyguladı mı. Sağlayıcıya ulaşılamazsa ödeme PENDING kalır; mutabakat job'ı sonra yeniden
     * sorar.
     */
    private boolean sonucuSorgulaVeUygula(Odeme odeme) {
        SorguSonucu sonuc;
        try {
            sonuc = saglayici.sonucuSorgula(odeme.getSaglayiciTokeni(), odeme.getKonusmaKimligi());
        } catch (BagimlilikKullanilamazIstisnasi e) {
            log.warn("Ödeme sonucu sorgulanamadı (ödeme {}); mutabakat yeniden deneyecek", odeme.getId());
            return false;
        }
        kaydet(odeme.getId(), odeme.getSaglayiciTokeni(), OlayKaynagi.RETRIEVE, sonuc.durum().name(),
                sonuc.maskeliGovde());
        return sonucIslemi.sonucuUygula(odeme.getId(), sonuc);
    }

    private void kaydet(Long odemeId, String token, OlayKaynagi kaynak, String sonucDurumu, String maskeliGovde) {
        kayitRepository.save(SaglayiciKaydi.olustur(odemeId, token, kaynak, sonucDurumu, maskeliGovde,
                saat.instant()));
    }

    private UriComponentsBuilder sonucSayfasi() {
        return UriComponentsBuilder.fromUri(ayarlar.sonucAdresi());
    }

    /** Callback'in denetim kaydı: yalnızca token (form alanları dışında bir şey kaydedilmez). */
    private static String callbackGovdesi(String token) {
        return "{\"token\":\"" + token.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }
}
