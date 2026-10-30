package com.sinema.catalog.tmdb.controller;

import com.sinema.catalog.tmdb.dto.TmdbAramaSonucu;
import com.sinema.catalog.tmdb.dto.TmdbFilmOnizlemesi;
import com.sinema.catalog.tmdb.service.TmdbServisi;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin film formu için TMDB araması ve ön doldurma (planlama §7.4). TMDB yoksa 503, elle giriş çalışır. */
@RestController
@RequestMapping("/api/v1/admin/catalog/tmdb")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TmdbYonetimKontrolcusu {

    /** TMDB arama sayfaları 1'den başlar ve en çok 500'dür. */
    private static final int TMDB_EN_SON_SAYFA = 500;

    private final TmdbServisi tmdbServisi;

    @GetMapping("/search")
    public TmdbAramaSonucu ara(
            @RequestParam @NotBlank @Size(max = 100) String query,
            @RequestParam(defaultValue = "1") @Min(1) @Max(TMDB_EN_SON_SAYFA) int page) {
        return tmdbServisi.ara(query, page);
    }

    @GetMapping("/movies/{tmdbId}")
    public TmdbFilmOnizlemesi onizleme(@PathVariable @Positive long tmdbId) {
        return tmdbServisi.onizleme(tmdbId);
    }
}
