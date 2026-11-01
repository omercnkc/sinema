package com.sinema.catalog.seans.controller;

import com.sinema.catalog.seans.dto.SeansDetayi;
import com.sinema.catalog.seans.dto.SeansGuncellemeIstegi;
import com.sinema.catalog.seans.dto.SeansOlusturmaIstegi;
import com.sinema.catalog.seans.dto.YonetimSeansOzeti;
import com.sinema.catalog.seans.service.SeansServisi;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Salon çizelgesi, seans ekleme ve fiyat/ofset güncelleme. Silme ve iptal yok (K34). */
@RestController
@RequestMapping("/api/v1/admin/catalog/showtimes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SeansYonetimKontrolcusu {

    private final SeansServisi seansServisi;

    /** Salonun günlük çizelgesi; tarih İstanbul günüdür, boşsa bugün. */
    @GetMapping
    public List<YonetimSeansOzeti> salonunSeanslari(@RequestParam Long hallId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return seansServisi.salonunSeanslari(hallId, date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeansDetayi olustur(@Valid @RequestBody SeansOlusturmaIstegi istek) {
        return seansServisi.olustur(istek);
    }

    @PutMapping("/{seansId}")
    public SeansDetayi guncelle(@PathVariable Long seansId, @Valid @RequestBody SeansGuncellemeIstegi istek) {
        return seansServisi.guncelle(seansId, istek);
    }
}
