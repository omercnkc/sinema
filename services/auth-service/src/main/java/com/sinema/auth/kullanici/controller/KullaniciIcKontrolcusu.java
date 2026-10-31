package com.sinema.auth.kullanici.controller;

import com.sinema.auth.kullanici.dto.AliciBilgisi;
import com.sinema.auth.kullanici.service.ProfilServisi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Servisler arası iç uç (K48): payment, ödeme başlatırken iyzico alıcı bilgisini buradan okur. Yetkiyi ortak güvenlik
 * zinciri uygular (/internal/** yalnızca SERVICE rolü ve aud=auth-service); auth bu hedefe yalnızca payment'a token
 * verir. Gateway bu yolu hiç yönlendirmez. Yalnızca okuma: auth kimseyi çağırmaz, servisler arası döngü oluşmaz.
 */
@RestController
@RequestMapping("/internal/v1/auth/users/{kullaniciId}")
@RequiredArgsConstructor
public class KullaniciIcKontrolcusu {

    private final ProfilServisi profilServisi;

    @GetMapping("/billing-info")
    public AliciBilgisi aliciBilgisi(@PathVariable Long kullaniciId) {
        return profilServisi.aliciBilgisi(kullaniciId);
    }
}
