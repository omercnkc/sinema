package com.sinema.auth.kullanici.controller;

import com.sinema.auth.kullanici.dto.KayitCevabi;
import com.sinema.auth.kullanici.dto.KayitIstegi;
import com.sinema.auth.kullanici.service.KullaniciKayitServisi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Yeni adminleri yalnızca mevcut bir admin ekler (planlama §5.6, K36). */
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminKontrolcusu {

    private final KullaniciKayitServisi kullaniciKayitServisi;

    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public KayitCevabi adminEkle(@Valid @RequestBody KayitIstegi istek) {
        return kullaniciKayitServisi.adminEkle(istek);
    }
}
