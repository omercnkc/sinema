package com.sinema.booking.rezervasyon.controller;

import com.sinema.booking.rezervasyon.dto.OdemeBilgisi;
import com.sinema.booking.rezervasyon.dto.OnayCevabi;
import com.sinema.booking.rezervasyon.dto.OnayIstegi;
import com.sinema.booking.rezervasyon.service.OdemeAkisiServisi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * payment-service'in çağırdığı iç uçlar (planlama §7.5). Yetkiyi ortak güvenlik zinciri uygular: /internal/**
 * yalnızca SERVICE rolü ve aud=booking-service olan token'a açık; auth bu hedefe yalnızca payment'a token verir.
 * Gateway bu yolu hiç yönlendirmez.
 */
@RestController
@RequestMapping("/internal/v1/bookings/reservations/{rezervasyonId}")
@RequiredArgsConstructor
public class RezervasyonIcKontrolcusu {

    private final OdemeAkisiServisi odemeAkisiServisi;

    @GetMapping("/payment-info")
    public OdemeBilgisi odemeBilgisi(@PathVariable Long rezervasyonId) {
        return odemeAkisiServisi.bilgi(rezervasyonId);
    }

    @PostMapping("/extend-payment-window")
    public OdemeBilgisi odemePenceresiniUzat(@PathVariable Long rezervasyonId) {
        return odemeAkisiServisi.odemePenceresiniUzat(rezervasyonId);
    }

    @PostMapping("/confirm")
    public OnayCevabi onayla(@PathVariable Long rezervasyonId, @Valid @RequestBody OnayIstegi istek) {
        return odemeAkisiServisi.onayla(rezervasyonId, istek.paymentId());
    }

    @PostMapping("/fail")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void basarisiz(@PathVariable Long rezervasyonId) {
        odemeAkisiServisi.basarisiz(rezervasyonId);
    }
}
