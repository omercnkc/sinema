package com.sinema.payment.kullanici.istemci;

import com.sinema.common.servisarasi.BagimlilikHatalari;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** auth'a ulaşılamazsa 503 AUTH_UNAVAILABLE; iş hatası (404 kullanıcı yok) aynen fırlatılır. */
@Slf4j
@Component
public class KullaniciIstemcisiYedegi implements FallbackFactory<KullaniciIstemcisi> {

    static final String KOD = "AUTH_UNAVAILABLE";
    private static final String MESAJ = "Ödeme şu an başlatılamıyor, lütfen biraz sonra tekrar deneyin.";

    @Override
    public KullaniciIstemcisi create(Throwable neden) {
        if (!BagimlilikHatalari.isSonucuMu(neden)) {
            log.warn("auth-service çağrısı başarısız: {}", neden.toString());
        }
        return kullaniciId -> {
            throw BagimlilikHatalari.cevir(neden, KOD, MESAJ);
        };
    }
}
