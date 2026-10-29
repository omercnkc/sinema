package com.sinema.auth.oturum.controller;

import com.sinema.auth.config.KimlikAyarlari;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/**
 * Refresh token cookie'sinin tüm ayarları tek yerde (planlama §5.1):
 *  - HttpOnly: JavaScript okuyamaz, XSS ile çalınamaz
 *  - Secure: yalnızca HTTPS'te gider (tarayıcılar localhost'u istisna sayar)
 *  - SameSite=Strict: başka siteden tetiklenen isteğe eklenmez (CSRF'e karşı)
 *  - Path=/api/v1/auth: yalnızca auth uçlarına gider, diğer API isteklerinde taşınmaz
 */
@Component
public class YenilemeCerezi {

    private final KimlikAyarlari.Yenileme ayarlar;

    public YenilemeCerezi(KimlikAyarlari ayarlar) {
        this.ayarlar = ayarlar.yenileme();
    }

    public ResponseCookie olustur(String hamToken) {
        return temel(hamToken).maxAge(ayarlar.sure()).build();
    }

    /** Aynı ad/yol ile boş değer ve Max-Age=0: tarayıcı cookie'yi siler. */
    public ResponseCookie sil() {
        return temel("").maxAge(Duration.ZERO).build();
    }

    public String oku(HttpServletRequest istek) {
        Cookie cerez = WebUtils.getCookie(istek, ayarlar.cerezAdi());
        return cerez != null ? cerez.getValue() : null;
    }

    private ResponseCookie.ResponseCookieBuilder temel(String deger) {
        return ResponseCookie.from(ayarlar.cerezAdi(), deger)
                .httpOnly(true)
                .secure(ayarlar.guvenliCerez())
                .sameSite("Strict")
                .path(ayarlar.cerezYolu());
    }
}
