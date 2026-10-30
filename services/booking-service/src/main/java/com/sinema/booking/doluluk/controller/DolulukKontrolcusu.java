package com.sinema.booking.doluluk.controller;

import com.sinema.booking.doluluk.dto.DolulukCevabi;
import com.sinema.booking.doluluk.service.DolulukServisi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Misafire açık doluluk ucu (K3, K40; açık yol application.yml'de). Tarayıcı ve ara katmanlar önbelleğe almaz
 * (no-store): tazeliği sunucudaki 2 sn'lik önbellek belirler, aksi halde polling eski veriyi tekrar tekrar görürdü.
 */
@RestController
@RequiredArgsConstructor
public class DolulukKontrolcusu {

    private final DolulukServisi dolulukServisi;

    @GetMapping("/api/v1/bookings/showtimes/{seansId}/seat-status")
    public ResponseEntity<DolulukCevabi> doluluk(@PathVariable Long seansId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(dolulukServisi.doluluk(seansId));
    }
}
