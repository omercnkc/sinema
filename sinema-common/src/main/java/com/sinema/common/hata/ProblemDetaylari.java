package com.sinema.common.hata;

import com.sinema.common.korelasyon.KorelasyonKimligi;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * Hata cevabının ortak alanları tek yerde: hem MVC hata yöneticisi hem de güvenlik filtreleri
 * (MVC'ye hiç girmeyen 401/403) aynı biçimi buradan üretir.
 */
public final class ProblemDetaylari {

    public static final String KOD_ALANI = "code";
    public static final String KORELASYON_ALANI = "correlationId";
    public static final String ZAMAN_ALANI = "timestamp";

    private ProblemDetaylari() {
    }

    public static ProblemDetail olustur(HttpStatusCode durum, String kod, String detay) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(durum, detay);
        problem.setProperty(KOD_ALANI, kod);
        zenginlestir(problem);
        return problem;
    }

    public static void zenginlestir(ProblemDetail problem) {
        problem.setProperty(KORELASYON_ALANI, MDC.get(KorelasyonKimligi.MDC_ANAHTARI));
        problem.setProperty(ZAMAN_ALANI, Instant.now().toString());
    }
}
