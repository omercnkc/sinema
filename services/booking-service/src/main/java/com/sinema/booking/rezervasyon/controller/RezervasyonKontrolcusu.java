package com.sinema.booking.rezervasyon.controller;

import com.sinema.booking.rezervasyon.dto.KoltukTutmaIstegi;
import com.sinema.booking.rezervasyon.dto.RezervasyonCevabi;
import com.sinema.booking.rezervasyon.service.KoltukTutmaServisi;
import com.sinema.booking.rezervasyon.service.RezervasyonServisi;
import com.sinema.common.sayfa.SayfaCevabi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    public static final int EN_BUYUK_SAYFA = 50;
    public static final String VARSAYILAN_SAYFA_BOYUTU = "20";

    private final KoltukTutmaServisi koltukTutmaServisi;
    private final RezervasyonServisi rezervasyonServisi;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RezervasyonCevabi koltukTut(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody KoltukTutmaIstegi istek) {
        return koltukTutmaServisi.tut(kullaniciId(token), istek);
    }

    /** Yalnızca sahibi görür; ödeme sonrası ön yüz durumu bu uçtan yoklar (§8.6). */
    @GetMapping("/{rezervasyonId}")
    public RezervasyonCevabi getir(@AuthenticationPrincipal Jwt token, @PathVariable Long rezervasyonId) {
        return rezervasyonServisi.getir(kullaniciId(token), rezervasyonId);
    }

    @GetMapping
    public SayfaCevabi<RezervasyonCevabi> listele(@AuthenticationPrincipal Jwt token,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = VARSAYILAN_SAYFA_BOYUTU) @Min(1) @Max(EN_BUYUK_SAYFA) int size) {
        return rezervasyonServisi.listele(kullaniciId(token), page, size);
    }

    /** Ödeme öncesi hold'u bırakır (K40); satın alınmış bilet iptal edilemez (K6). */
    @DeleteMapping("/{rezervasyonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void birak(@AuthenticationPrincipal Jwt token, @PathVariable Long rezervasyonId) {
        rezervasyonServisi.birak(kullaniciId(token), rezervasyonId);
    }

    private static Long kullaniciId(Jwt token) {
        return Long.valueOf(token.getSubject());
    }
}
