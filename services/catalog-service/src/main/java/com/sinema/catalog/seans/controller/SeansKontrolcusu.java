package com.sinema.catalog.seans.controller;

import com.sinema.catalog.seans.dto.SeansDetayi;
import com.sinema.catalog.seans.dto.SeansDuzeni;
import com.sinema.catalog.seans.dto.SeansOzeti;
import com.sinema.catalog.seans.service.SeansServisi;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/** Herkese açık seans uçları (misafir dahil, K3). */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class SeansKontrolcusu {

    /**
     * Düzen sürümü değişmez ama K35 göçü (M8) seansı yeni sürüme taşıyabilir; bu yüzden uzun süreli değil,
     * kısa önbellek + ETag: süre dolunca istemci "değişti mi?" diye sorar, değişmediyse gövdesiz 304 alır.
     */
    private static final CacheControl DUZEN_ONBELLEGI = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final SeansServisi seansServisi;

    @GetMapping("/movies/{filmId}/showtimes")
    public List<SeansOzeti> filminSeanslari(@PathVariable Long filmId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long cinemaId) {
        return seansServisi.filminSeanslari(filmId, date, cinemaId);
    }

    @GetMapping("/showtimes/{seansId}")
    public SeansDetayi detay(@PathVariable Long seansId) {
        return seansServisi.detay(seansId);
    }

    /** ETag = düzen id'si: seansın düzeni değişmedikçe aynı kalır. */
    @GetMapping("/showtimes/{seansId}/layout")
    public ResponseEntity<SeansDuzeni> duzen(@PathVariable Long seansId, WebRequest istek) {
        SeansDuzeni duzen = seansServisi.duzen(seansId);
        String etiket = "\"layout-" + duzen.layoutId() + "\"";
        if (istek.checkNotModified(etiket)) {
            return null; // Spring 304 yazar
        }
        return ResponseEntity.ok().eTag(etiket).cacheControl(DUZEN_ONBELLEGI).body(duzen);
    }
}
