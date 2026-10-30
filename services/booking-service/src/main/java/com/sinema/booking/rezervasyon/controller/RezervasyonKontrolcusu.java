package com.sinema.booking.rezervasyon.controller;

import com.sinema.booking.rezervasyon.dto.KoltukTutmaIstegi;
import com.sinema.booking.rezervasyon.dto.RezervasyonCevabi;
import com.sinema.booking.rezervasyon.service.KoltukTutmaServisi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kullanıcının rezervasyon uçları (planlama §7.5). Giriş zorunlu: ortak güvenlik zinciri /api/v1/** için
 * USER/ADMIN token'ı ister (misafir 401, K3). Kullanıcı kimliği yalnızca token'dan ("sub") alınır, gövdeden değil.
 */
@RestController
@RequestMapping("/api/v1/bookings/reservations")
@RequiredArgsConstructor
public class RezervasyonKontrolcusu {

    private final KoltukTutmaServisi koltukTutmaServisi;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RezervasyonCevabi koltukTut(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody KoltukTutmaIstegi istek) {
        return koltukTutmaServisi.tut(Long.valueOf(token.getSubject()), istek);
    }
}
