package com.sinema.common.guvenlik;

import com.sinema.common.hata.ProblemDetaylari;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 401: korumalı adrese token'sız ya da geçersiz/süresi dolmuş token'la gelindi.
 * Spring Security'nin varsayılanı boş gövde döner; bunun yerine ortak hata biçimi yazılır.
 * Ayrıntı (imza mı bozuk, süre mi dolmuş) istemciye verilmez.
 */
public class ProblemJsonGirisNoktasi implements AuthenticationEntryPoint {

    public static final String KOD = "UNAUTHORIZED";

    private final ProblemJsonYazici yazici;

    public ProblemJsonGirisNoktasi(ProblemJsonYazici yazici) {
        this.yazici = yazici;
    }

    @Override
    public void commence(HttpServletRequest istek, HttpServletResponse cevap, AuthenticationException istisna)
            throws IOException {
        cevap.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        yazici.yaz(istek, cevap, ProblemDetaylari.olustur(HttpStatus.UNAUTHORIZED, KOD,
                "Bu işlem için geçerli bir oturum gerekli."));
    }
}
