package com.sinema.booking.katalog.istemci;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * catalog-service'in iç uçları (planlama §3.3, §7.4). İstemci adı çağrılan servisin adıdır: servis token'ının
 * "aud" değeri buradan alınır (sinema-common ServisTokeniEkleyici). Token, korelasyon kimliği, zaman aşımı,
 * GET'e yeniden deneme ve devre kesici ortak altyapıdan gelir; burada yalnızca sözleşme var.
 *
 * Hatalar: seans yoksa KaynakBulunamadiIstisnasi (catalog'un kodu ve mesajıyla), catalog'a ulaşılamazsa
 * 503 CATALOG_UNAVAILABLE (KatalogIstemcisiYedegi). Hold açılmaz, fail fast (§3.3).
 */
@FeignClient(name = "catalog-service", url = "${sinema.servisler.catalog-adresi}",
        fallbackFactory = KatalogIstemcisiYedegi.class)
public interface KatalogIstemcisi {

    @GetMapping("/internal/v1/catalog/showtimes/{seansId}")
    KatalogSeansBilgisi seansBilgisi(@PathVariable("seansId") Long seansId);

    /** Seçilen hücreler bu seansın mühürlü düzeninde koltuk mu? Bir şey değiştirmez ama POST (gövdeli sorgu). */
    @PostMapping("/internal/v1/catalog/showtimes/{seansId}/seats/resolve")
    KoltukCozumlemeSonucu koltuklariCoz(@PathVariable("seansId") Long seansId,
            @RequestBody KoltukCozumlemeIstegi istek);
}
