package com.sinema.booking.bildirim.controller;

import com.sinema.booking.bildirim.dto.BildirimCevabi;
import com.sinema.booking.bildirim.dto.OkunmamisSayisi;
import com.sinema.booking.bildirim.service.BildirimServisi;
import com.sinema.booking.rezervasyon.controller.RezervasyonKontrolcusu;
import com.sinema.common.sayfa.SayfaCevabi;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Uygulama içi bildirimler (K35): yalnızca giriş yapmış kullanıcının kendi bildirimleri. */
@Validated
@RestController
@RequestMapping("/api/v1/bookings/notifications")
@RequiredArgsConstructor
public class BildirimKontrolcusu {

    private final BildirimServisi bildirimServisi;

    @GetMapping
    public SayfaCevabi<BildirimCevabi> listele(@AuthenticationPrincipal Jwt token,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = RezervasyonKontrolcusu.VARSAYILAN_SAYFA_BOYUTU) @Min(1)
            @Max(RezervasyonKontrolcusu.EN_BUYUK_SAYFA) int size) {
        return bildirimServisi.listele(kullaniciId(token), page, size);
    }

    @GetMapping("/unread-count")
    public OkunmamisSayisi okunmamisSayisi(@AuthenticationPrincipal Jwt token) {
        return bildirimServisi.okunmamisSayisi(kullaniciId(token));
    }

    /** Tekrar çağrı zararsızdır (204). */
    @PostMapping("/{bildirimId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void okundu(@AuthenticationPrincipal Jwt token, @PathVariable Long bildirimId) {
        bildirimServisi.okundu(kullaniciId(token), bildirimId);
    }

    private static Long kullaniciId(Jwt token) {
        return Long.valueOf(token.getSubject());
    }
}
