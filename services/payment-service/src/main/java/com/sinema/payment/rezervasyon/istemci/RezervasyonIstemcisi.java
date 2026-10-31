package com.sinema.payment.rezervasyon.istemci;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * booking-service'in ödeme için iç uçları (planlama §7.5). İstemci adı çağrılan servisin adıdır: servis token'ının
 * "aud" değeri buradan alınır. Token, korelasyon kimliği, zaman aşımı, GET'e yeniden deneme ve devre kesici ortak
 * altyapıdan gelir (sinema-common).
 *
 * İş sonuçları istisna olarak gelir ve booking'in kodunu taşır: 404 rezervasyon yok, 409 süre dolmuş/kapanmış ya da
 * zaten onaylı (RESERVATION_NOT_ACTIVE, RESERVATION_ALREADY_CONFIRMED). booking'e ulaşılamazsa 503 BOOKING_UNAVAILABLE.
 * confirm/fail booking'de tekrarlanabilir (idempotent): aynı çağrının tekrarı sonucu değiştirmez.
 */
@FeignClient(name = "booking-service", url = "${sinema.servisler.booking-adresi}",
        fallbackFactory = RezervasyonIstemcisiYedegi.class)
public interface RezervasyonIstemcisi {

    String YOL = "/internal/v1/bookings/reservations/{rezervasyonId}";

    @GetMapping(YOL + "/payment-info")
    RezervasyonOdemeBilgisi odemeBilgisi(@PathVariable("rezervasyonId") Long rezervasyonId);

    /** K32: hold bir kez uzar; tekrar çağrı mevcut durumu döner. */
    @PostMapping(YOL + "/extend-payment-window")
    RezervasyonOdemeBilgisi odemePenceresiniUzat(@PathVariable("rezervasyonId") Long rezervasyonId);

    @PostMapping(YOL + "/confirm")
    OnaySonucu onayla(@PathVariable("rezervasyonId") Long rezervasyonId, @RequestBody OnayIstegi istek);

    @PostMapping(YOL + "/fail")
    void basarisiz(@PathVariable("rezervasyonId") Long rezervasyonId);
}
