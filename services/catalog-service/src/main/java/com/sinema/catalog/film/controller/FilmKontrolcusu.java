package com.sinema.catalog.film.controller;

import com.sinema.catalog.film.dto.FilmDetayi;
import com.sinema.catalog.film.dto.FilmOzeti;
import com.sinema.catalog.film.service.FilmServisi;
import com.sinema.common.sayfa.SayfaCevabi;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Herkese açık film listesi ve detayı (misafir dahil, K3). Gizli filmler görünmez.
 * Parametre kısıtları Spring'in yerleşik metot doğrulamasıyla 400'e çevrilir.
 */
@RestController
@RequestMapping("/api/v1/catalog/movies")
@RequiredArgsConstructor
public class FilmKontrolcusu {

    /** Sayfa boyutu üst sınırı (planlama §7.2). */
    public static final int EN_BUYUK_SAYFA = 50;
    public static final String VARSAYILAN_SAYFA_BOYUTU = "20";

    private final FilmServisi filmServisi;

    @GetMapping
    public SayfaCevabi<FilmOzeti> listele(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) @Size(max = 50) String genre,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = VARSAYILAN_SAYFA_BOYUTU) @Min(1) @Max(EN_BUYUK_SAYFA) int size) {
        return filmServisi.herkeseAcikAra(q, genre, page, size);
    }

    @GetMapping("/{filmId}")
    public FilmDetayi getir(@PathVariable Long filmId) {
        return filmServisi.herkeseAcikGetir(filmId);
    }
}
