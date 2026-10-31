package com.sinema.booking.bilet.controller;

import com.sinema.booking.bilet.dto.BiletDogrulamaCevabi;
import com.sinema.booking.bilet.service.BiletDogrulamaServisi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * QR bilet doğrulama ucu (K30): herkese açık (açık yol application.yml'de, gateway'de hız sınırlı). Cevap önbelleğe
 * alınmaz: biletin durumu doğrulama anına aittir.
 */
@RestController
@RequiredArgsConstructor
public class BiletDogrulamaKontrolcusu {

    private final BiletDogrulamaServisi biletDogrulamaServisi;

    @GetMapping("/api/v1/tickets/{kod}")
    public ResponseEntity<BiletDogrulamaCevabi> dogrula(@PathVariable String kod) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(biletDogrulamaServisi.dogrula(kod));
    }
}
