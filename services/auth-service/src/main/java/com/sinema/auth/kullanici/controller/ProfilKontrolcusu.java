package com.sinema.auth.kullanici.controller;

import com.sinema.auth.kullanici.dto.ProfilCevabi;
import com.sinema.auth.kullanici.service.ProfilServisi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class ProfilKontrolcusu {

    private final ProfilServisi profilServisi;

    // Kimlik istekten değil doğrulanmış token'dan (sub) alınır: kimse başkasının profilini isteyemez
    @GetMapping("/me")
    public ProfilCevabi ben(@AuthenticationPrincipal Jwt token) {
        return profilServisi.profil(Long.valueOf(token.getSubject()));
    }
}
