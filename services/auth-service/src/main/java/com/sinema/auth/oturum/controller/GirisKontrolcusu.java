package com.sinema.auth.oturum.controller;

import com.sinema.auth.oturum.dto.GirisCevabi;
import com.sinema.auth.oturum.dto.GirisIstegi;
import com.sinema.auth.oturum.service.GirisServisi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class GirisKontrolcusu {

    private final GirisServisi girisServisi;

    @PostMapping("/login")
    public GirisCevabi girisYap(@Valid @RequestBody GirisIstegi istek) {
        return girisServisi.girisYap(istek);
    }
}
