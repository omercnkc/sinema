package com.sinema.auth.oturum.controller;

import com.sinema.auth.oturum.dto.GirisCevabi;
import com.sinema.auth.oturum.dto.GirisIstegi;
import com.sinema.auth.oturum.service.GirisServisi;
import com.sinema.auth.oturum.service.OturumBilgisi;
import com.sinema.auth.oturum.service.YenilemeServisi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Oturum uçları: giriş, yenileme, çıkış (planlama §7.3). Access token gövdede, refresh token cookie'de. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class OturumKontrolcusu {

    private final GirisServisi girisServisi;
    private final YenilemeServisi yenilemeServisi;
    private final YenilemeCerezi yenilemeCerezi;

    @PostMapping("/login")
    public ResponseEntity<GirisCevabi> girisYap(@Valid @RequestBody GirisIstegi istek) {
        return oturumCevabi(girisServisi.girisYap(istek));
    }

    @PostMapping("/refresh")
    public ResponseEntity<GirisCevabi> yenile(HttpServletRequest istek) {
        return oturumCevabi(yenilemeServisi.yenile(yenilemeCerezi.oku(istek)));
    }

    // Access token süresi dolana kadar (≤15 dk) geçerli kalır; kara liste yok, kabul edilen risk (planlama §5.3)
    @PostMapping("/logout")
    public ResponseEntity<Void> cikisYap(HttpServletRequest istek) {
        yenilemeServisi.cikisYap(yenilemeCerezi.oku(istek));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, yenilemeCerezi.sil().toString())
                .build();
    }

    private ResponseEntity<GirisCevabi> oturumCevabi(OturumBilgisi oturum) {
        ResponseEntity.BodyBuilder cevap = ResponseEntity.ok();
        if (oturum.yenilemeTokeni() != null) {
            cevap.header(HttpHeaders.SET_COOKIE, yenilemeCerezi.olustur(oturum.yenilemeTokeni()).toString());
        }
        return cevap.body(new GirisCevabi(oturum.erisimTokeni().deger(), oturum.erisimTokeni().gecerlilik().toSeconds()));
    }
}
