package com.sinema.payment.odeme.controller;

import com.sinema.payment.odeme.dto.OdemeBaslatmaIstegi;
import com.sinema.payment.odeme.dto.OdemeCevabi;
import com.sinema.payment.odeme.service.OdemeBaslatmaServisi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kullanıcının ödeme uçları (planlama §7.6). Giriş zorunlu (ortak güvenlik zinciri); kullanıcı kimliği yalnızca
 * token'dan ("sub") alınır.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class OdemeKontrolcusu {

    private final OdemeBaslatmaServisi odemeBaslatmaServisi;

    /** Yeni ödeme 201; aynı rezervasyonun sayfası hâlâ geçerliyse aynı ödeme 200 ile döner. */
    @PostMapping
    public ResponseEntity<OdemeCevabi> baslat(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody OdemeBaslatmaIstegi istek) {
        OdemeBaslatmaServisi.Sonuc sonuc = odemeBaslatmaServisi.baslat(kullaniciId(token), istek.reservationId());
        return ResponseEntity.status(sonuc.yeni() ? HttpStatus.CREATED : HttpStatus.OK).body(sonuc.cevap());
    }

    @GetMapping("/by-reservation/{rezervasyonId}")
    public OdemeCevabi rezervasyonOdemesi(@AuthenticationPrincipal Jwt token, @PathVariable Long rezervasyonId) {
        return odemeBaslatmaServisi.rezervasyonOdemesi(kullaniciId(token), rezervasyonId);
    }

    private static Long kullaniciId(Jwt token) {
        return Long.valueOf(token.getSubject());
    }
}
