package com.sinema.catalog.seans.controller;

import com.sinema.catalog.seans.dto.IcSeansBilgisi;
import com.sinema.catalog.seans.dto.KoltukCozumlemeCevabi;
import com.sinema.catalog.seans.dto.KoltukCozumlemeIstegi;
import com.sinema.catalog.seans.service.SeansServisi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Servisler arası uçlar (planlama §5.5, §7.5). Yetkiyi ortak güvenlik zinciri uygular: /internal/** yalnızca
 * SERVICE rolü ve aud=catalog-service olan token'a açık; gateway bu yolu hiç yönlendirmez.
 * Kullanıcı bağlamı yok: booking, kullanıcının yetkisini kendisi kontrol etmiş olarak çağırır.
 */
@RestController
@RequestMapping("/internal/v1/catalog/showtimes/{seansId}")
@RequiredArgsConstructor
public class SeansIcKontrolcusu {

    private final SeansServisi seansServisi;

    @GetMapping
    public IcSeansBilgisi bilgi(@PathVariable Long seansId) {
        return seansServisi.icBilgi(seansId);
    }

    /** Gövdeli sorgu olduğu için POST; bir şey değiştirmez (güvenli ve tekrarlanabilir). */
    @PostMapping("/seats/resolve")
    public KoltukCozumlemeCevabi koltuklariCoz(@PathVariable Long seansId,
            @Valid @RequestBody KoltukCozumlemeIstegi istek) {
        return seansServisi.koltuklariCoz(seansId, istek.cellIds());
    }
}
