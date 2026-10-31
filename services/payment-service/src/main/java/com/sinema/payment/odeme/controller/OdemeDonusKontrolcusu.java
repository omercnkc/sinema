package com.sinema.payment.odeme.controller;

import com.sinema.payment.odeme.service.OdemeDonusServisi;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sağlayıcının ödeme sonrası tarayıcıyı yönlendirdiği adres (K39): iyzico (ya da mock sayfası) token'ı form POST'uyla
 * gönderir. JWT yoktur (açık yol); güvence sonucun sağlayıcıdan sorgulanmasıdır (OdemeDonusServisi). Cevap her zaman
 * ön yüzün sonuç sayfasına 302 yönlendirmesidir.
 */
@RestController
@RequiredArgsConstructor
public class OdemeDonusKontrolcusu {

    private final OdemeDonusServisi odemeDonusServisi;

    @PostMapping(path = "/api/v1/payments/callback/iyzico", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> donus(@RequestParam @NotBlank @Size(max = 255) String token) {
        return ResponseEntity.status(HttpStatus.FOUND).location(odemeDonusServisi.isle(token)).build();
    }
}
