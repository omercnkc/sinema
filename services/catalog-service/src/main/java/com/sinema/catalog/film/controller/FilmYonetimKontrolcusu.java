package com.sinema.catalog.film.controller;

import com.sinema.catalog.film.dto.FilmDetayi;
import com.sinema.catalog.film.dto.FilmIstegi;
import com.sinema.catalog.film.dto.FilmOzeti;
import com.sinema.catalog.film.dto.GorunurlukIstegi;
import com.sinema.catalog.film.dto.SayfaCevabi;
import com.sinema.catalog.film.service.FilmServisi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Film yönetimi: gizli filmler de listelenir ve düzenlenebilir. */
@RestController
@RequestMapping("/api/v1/admin/catalog/movies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FilmYonetimKontrolcusu {

    private final FilmServisi filmServisi;

    @GetMapping
    public SayfaCevabi<FilmOzeti> listele(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = FilmKontrolcusu.VARSAYILAN_SAYFA_BOYUTU) @Min(1)
            @Max(FilmKontrolcusu.EN_BUYUK_SAYFA) int size) {
        return filmServisi.yonetimIcinAra(q, page, size);
    }

    @GetMapping("/{filmId}")
    public FilmDetayi getir(@PathVariable Long filmId) {
        return filmServisi.yonetimIcinGetir(filmId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FilmDetayi olustur(@Valid @RequestBody FilmIstegi istek) {
        return filmServisi.olustur(istek);
    }

    @PutMapping("/{filmId}")
    public FilmDetayi guncelle(@PathVariable Long filmId, @Valid @RequestBody FilmIstegi istek) {
        return filmServisi.guncelle(filmId, istek);
    }

    @PatchMapping("/{filmId}/visibility")
    public FilmDetayi gorunurlukDegistir(@PathVariable Long filmId, @Valid @RequestBody GorunurlukIstegi istek) {
        return filmServisi.gorunurlukDegistir(filmId, istek.visible());
    }
}
