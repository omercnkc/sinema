package com.sinema.auth.servis.controller;

import com.sinema.auth.servis.dto.ServisTokeniCevabi;
import com.sinema.auth.servis.dto.ServisTokeniIstegi;
import com.sinema.auth.servis.service.ServisTokeniServisi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * İç uç: gateway /internal/** yollarını hiç yönlendirmez, yalnızca Docker iç ağından erişilir (planlama §5.5).
 * JWT istemez (token almanın yolu budur); kimlik clientSecret ile kanıtlanır.
 */
@RestController
@RequestMapping("/internal/v1/auth")
@RequiredArgsConstructor
public class ServisTokeniKontrolcusu {

    private final ServisTokeniServisi servisTokeniServisi;

    @PostMapping("/service-token")
    public ServisTokeniCevabi tokenVer(@Valid @RequestBody ServisTokeniIstegi istek) {
        return servisTokeniServisi.tokenVer(istek);
    }
}
