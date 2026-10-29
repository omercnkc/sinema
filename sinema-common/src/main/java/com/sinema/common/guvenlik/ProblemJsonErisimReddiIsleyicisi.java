package com.sinema.common.guvenlik;

import com.sinema.common.hata.ProblemDetaylari;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/** 403: kimlik geçerli ama rol yetmiyor (ör. USER admin adresine geldi); filtre seviyesinde. */
public class ProblemJsonErisimReddiIsleyicisi implements AccessDeniedHandler {

    public static final String KOD = "FORBIDDEN";
    static final String MESAJ = "Bu işlem için yetkiniz yok.";

    private final ProblemJsonYazici yazici;

    public ProblemJsonErisimReddiIsleyicisi(ProblemJsonYazici yazici) {
        this.yazici = yazici;
    }

    @Override
    public void handle(HttpServletRequest istek, HttpServletResponse cevap, AccessDeniedException istisna)
            throws IOException {
        yazici.yaz(istek, cevap, ProblemDetaylari.olustur(HttpStatus.FORBIDDEN, KOD, MESAJ));
    }
}
