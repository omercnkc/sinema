package com.sinema.catalog.duzen.controller;

import com.sinema.catalog.duzen.dto.DuzenEtkinlestirmeCevabi;
import com.sinema.catalog.duzen.dto.DuzenEtkinlestirmeIstegi;
import com.sinema.catalog.duzen.dto.GocKaynagi;
import com.sinema.catalog.duzen.service.DuzenGocuServisi;
import com.sinema.catalog.duzen.service.DuzenServisi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bilet göçü için servisler arası uçlar (K35, §7.8). /internal/** yalnızca SERVICE rolü ve aud=catalog-service
 * olan token'a açık (ortak güvenlik zinciri); gateway bu yolu yönlendirmez. Göçü booking yönetir.
 */
@RestController
@RequestMapping("/internal/v1/catalog/halls/{salonId}")
@RequiredArgsConstructor
public class DuzenIcKontrolcusu {

    private final DuzenGocuServisi gocServisi;
    private final DuzenServisi duzenServisi;

    @GetMapping("/migration-source")
    public GocKaynagi gocKaynagi(@PathVariable Long salonId, @RequestParam Long toLayoutId) {
        return gocServisi.gocKaynagi(salonId, toLayoutId);
    }

    @PostMapping("/layouts/{duzenId}/activate")
    public DuzenEtkinlestirmeCevabi etkinlestir(@PathVariable Long salonId, @PathVariable Long duzenId,
            @Valid @RequestBody DuzenEtkinlestirmeIstegi istek) {
        return duzenServisi.etkinlestir(salonId, duzenId, istek.showtimeIds());
    }
}
