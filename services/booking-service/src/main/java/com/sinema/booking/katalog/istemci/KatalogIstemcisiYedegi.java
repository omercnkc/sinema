package com.sinema.booking.katalog.istemci;

import com.sinema.common.servisarasi.BagimlilikHatalari;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Devre kesici her başarısız çağrıyı buraya yönlendirir (iş hatası olan 404 dahil). Karar ortak kuraldadır:
 * iş hatası aynen fırlatılır, catalog'a ulaşılamaması (ağ, 5xx, açık devre) 503 CATALOG_UNAVAILABLE olur.
 * Yedek veri dönülmez: seans/koltuk bilgisi olmadan hold açmak K9'u riske atar (fail fast).
 */
@Slf4j
@Component
public class KatalogIstemcisiYedegi implements FallbackFactory<KatalogIstemcisi> {

    static final String KOD = "CATALOG_UNAVAILABLE";
    private static final String MESAJ = "Seans bilgisi şu an alınamıyor, lütfen biraz sonra tekrar deneyin.";

    @Override
    public KatalogIstemcisi create(Throwable neden) {
        if (!BagimlilikHatalari.isSonucuMu(neden)) {
            log.warn("catalog-service çağrısı başarısız: {}", neden.toString());
        }
        return new KatalogIstemcisi() {
            @Override
            public KatalogSeansBilgisi seansBilgisi(Long seansId) {
                throw BagimlilikHatalari.cevir(neden, KOD, MESAJ);
            }

            @Override
            public KoltukCozumlemeSonucu koltuklariCoz(Long seansId, KoltukCozumlemeIstegi istek) {
                throw BagimlilikHatalari.cevir(neden, KOD, MESAJ);
            }
        };
    }
}
