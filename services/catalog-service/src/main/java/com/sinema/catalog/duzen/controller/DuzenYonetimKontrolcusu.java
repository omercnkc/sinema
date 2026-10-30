package com.sinema.catalog.duzen.controller;

import com.sinema.catalog.duzen.dto.DuzenCevabi;
import com.sinema.catalog.duzen.dto.DuzenIstegi;
import com.sinema.catalog.duzen.dto.DuzenOzeti;
import com.sinema.catalog.duzen.service.DuzenServisi;
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

/** Salon düzeni sürümleri (planlama §7.4). Yayımlama alt görev 3'te eklenir. */
@RestController
@RequestMapping("/api/v1/admin/catalog/halls/{salonId}/layouts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DuzenYonetimKontrolcusu {

    private final DuzenServisi duzenServisi;

    @GetMapping
    public List<DuzenOzeti> listele(@PathVariable Long salonId) {
        return duzenServisi.listele(salonId);
    }

    @GetMapping("/{duzenId}")
    public DuzenCevabi getir(@PathVariable Long salonId, @PathVariable Long duzenId) {
        return duzenServisi.getir(salonId, duzenId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DuzenCevabi taslakOlustur(@PathVariable Long salonId, @Valid @RequestBody DuzenIstegi istek) {
        return duzenServisi.taslakOlustur(salonId, istek);
    }

    @PutMapping("/{duzenId}")
    public DuzenCevabi taslakGuncelle(@PathVariable Long salonId, @PathVariable Long duzenId,
            @Valid @RequestBody DuzenIstegi istek) {
        return duzenServisi.taslakGuncelle(salonId, duzenId, istek);
    }
}
