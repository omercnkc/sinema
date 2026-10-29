package com.sinema.auth.kullanici.controller;

import com.sinema.auth.kullanici.dto.KayitCevabi;
import com.sinema.auth.kullanici.dto.KayitIstegi;
import com.sinema.auth.kullanici.service.KullaniciKayitServisi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class KayitKontrolcusu {

    private final KullaniciKayitServisi kullaniciKayitServisi;

    // Başarılı kayıt otomatik giriş yapmaz; istemci giriş sayfasına yönlenir (planlama §5.3)
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public KayitCevabi kaydet(@Valid @RequestBody KayitIstegi istek) {
        return kullaniciKayitServisi.kaydet(istek);
    }
}
