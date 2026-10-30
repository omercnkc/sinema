package com.sinema.catalog.sube.controller;

import com.sinema.catalog.sube.dto.SalonCevabi;
import com.sinema.catalog.sube.dto.SalonGuncellemeIstegi;
import com.sinema.catalog.sube.dto.SalonOlusturmaIstegi;
import com.sinema.catalog.sube.dto.SubeIstegi;
import com.sinema.catalog.sube.dto.YonetimSubeCevabi;
import com.sinema.catalog.sube.service.SubeServisi;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Şube/salon yönetimi. /api/v1/admin/** zaten ADMIN ister; @PreAuthorize ikinci kat güvencedir. */
@RestController
@RequestMapping("/api/v1/admin/catalog")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SubeYonetimKontrolcusu {

    private final SubeServisi subeServisi;

    @GetMapping("/cinemas")
    public List<YonetimSubeCevabi> subeleriListele() {
        return subeServisi.yonetimIcinListele();
    }

    @PostMapping("/cinemas")
    @ResponseStatus(HttpStatus.CREATED)
    public YonetimSubeCevabi subeOlustur(@Valid @RequestBody SubeIstegi istek) {
        return subeServisi.subeOlustur(istek);
    }

    @PutMapping("/cinemas/{subeId}")
    public YonetimSubeCevabi subeGuncelle(@PathVariable Long subeId, @Valid @RequestBody SubeIstegi istek) {
        return subeServisi.subeGuncelle(subeId, istek);
    }

    @PostMapping("/halls")
    @ResponseStatus(HttpStatus.CREATED)
    public SalonCevabi salonOlustur(@Valid @RequestBody SalonOlusturmaIstegi istek) {
        return subeServisi.salonOlustur(istek);
    }

    @PutMapping("/halls/{salonId}")
    public SalonCevabi salonGuncelle(@PathVariable Long salonId, @Valid @RequestBody SalonGuncellemeIstegi istek) {
        return subeServisi.salonGuncelle(salonId, istek);
    }
}
