package com.sinema.payment.rezervasyon.istemci;

import com.sinema.common.servisarasi.BagimlilikHatalari;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Başarısız booking çağrıları buraya gelir (iş hatası olan 404/409 dahil). Ortak kural: iş hatası aynen fırlatılır,
 * booking'e ulaşılamaması 503 BOOKING_UNAVAILABLE olur. Yedek veri dönülmez: tutar ve durum bilinmeden ödeme
 * başlatılmaz; onay iletilemezse mutabakat job'ı yeniden dener (§4.8).
 */
@Slf4j
@Component
public class RezervasyonIstemcisiYedegi implements FallbackFactory<RezervasyonIstemcisi> {

    static final String KOD = "BOOKING_UNAVAILABLE";
    private static final String MESAJ = "Rezervasyon bilgisi şu an alınamıyor, lütfen biraz sonra tekrar deneyin.";

    @Override
    public RezervasyonIstemcisi create(Throwable neden) {
        if (!BagimlilikHatalari.isSonucuMu(neden)) {
            log.warn("booking-service çağrısı başarısız: {}", neden.toString());
        }
        return new RezervasyonIstemcisi() {
            @Override
            public RezervasyonOdemeBilgisi odemeBilgisi(Long rezervasyonId) {
                throw BagimlilikHatalari.cevir(neden, KOD, MESAJ);
            }

            @Override
            public RezervasyonOdemeBilgisi odemePenceresiniUzat(Long rezervasyonId) {
                throw BagimlilikHatalari.cevir(neden, KOD, MESAJ);
            }

            @Override
            public OnaySonucu onayla(Long rezervasyonId, OnayIstegi istek) {
                throw BagimlilikHatalari.cevir(neden, KOD, MESAJ);
            }

            @Override
            public void basarisiz(Long rezervasyonId) {
                throw BagimlilikHatalari.cevir(neden, KOD, MESAJ);
            }
        };
    }
}
