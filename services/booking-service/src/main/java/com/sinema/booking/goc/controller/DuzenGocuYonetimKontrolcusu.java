package com.sinema.booking.goc.controller;

import com.sinema.booking.goc.dto.GocOnizlemeIstegi;
import com.sinema.booking.goc.dto.GocRaporu;
import com.sinema.booking.goc.service.GocPlanlamaServisi;
import com.sinema.booking.goc.service.GocRaporuOlusturucu;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Salon düzeni göçü (K35, §7.8): admin catalog'da taslağı hazırlar, burada önizler ve uygular. /api/v1/admin/**
 * zaten ADMIN ister; @PreAuthorize ikinci kat güvencedir.
 */
@RestController
@RequestMapping("/api/v1/admin/bookings/layout-migrations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DuzenGocuYonetimKontrolcusu {

    private final GocPlanlamaServisi planlamaServisi;
    private final GocRaporuOlusturucu raporOlusturucu;

    /** Hiçbir şey yazmaz; gövdeli hesap olduğu için POST. */
    @PostMapping("/preview")
    public GocRaporu onizle(@Valid @RequestBody GocOnizlemeIstegi istek) {
        return raporOlusturucu.rapor(istek.hallId(), istek.toLayoutId(),
                planlamaServisi.hesapla(istek.hallId(), istek.toLayoutId()));
    }
}
