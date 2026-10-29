package com.sinema.common.hata;

import com.sinema.common.korelasyon.KorelasyonKimligi;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Tüm hata cevaplarını tek biçime (RFC 7807 application/problem+json) çevirir.
 * Spring MVC'nin kendi hataları (bozuk JSON, yanlış metot, bilinmeyen adres...) üst sınıfta,
 * bizim istisnalarımız ve beklenmeyen hatalar burada ele alınır; hepsi aynı ek alanlarla zenginleştirilir.
 */
@RestControllerAdvice
public class GlobalHataYoneticisi extends ResponseEntityExceptionHandler {

    public static final String KOD_ALANI = "code";
    public static final String KORELASYON_ALANI = "correlationId";
    public static final String ZAMAN_ALANI = "timestamp";
    public static final String ALAN_HATALARI = "errors";

    public static final String DOGRULAMA_HATASI_KODU = "VALIDATION_FAILED";
    public static final String BEKLENMEYEN_HATA_KODU = "INTERNAL_ERROR";

    private static final Logger log = LoggerFactory.getLogger(GlobalHataYoneticisi.class);

    @ExceptionHandler(SinemaIstisnasi.class)
    public ResponseEntity<ProblemDetail> sinemaIstisnasiniIsle(SinemaIstisnasi istisna) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(istisna.getDurum(), istisna.getMessage());
        problem.setProperty(KOD_ALANI, istisna.getKod());
        istisna.getEkAlanlar().forEach(problem::setProperty);
        return problemCevabi(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> beklenmeyenHatayiIsle(Exception istisna) {
        // Ayrıntı yalnızca loga gider (korelasyon kimliğiyle); istemci genel bir mesaj görür
        log.error("Beklenmeyen hata", istisna);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.");
        problem.setProperty(KOD_ALANI, BEKLENMEYEN_HATA_KODU);
        return problemCevabi(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException istisna,
            HttpHeaders basliklar, HttpStatusCode durum, WebRequest istek) {
        ProblemDetail problem = istisna.getBody();
        problem.setDetail("İstekte geçersiz alanlar var.");
        problem.setProperty(KOD_ALANI, DOGRULAMA_HATASI_KODU);
        problem.setProperty(ALAN_HATALARI, alanHatalari(istisna));
        return handleExceptionInternal(istisna, problem, basliklar, durum, istek);
    }

    /** Üst sınıfın ürettiği tüm cevaplar buradan geçer: kodu olmayanlara HTTP durum adı kod olarak verilir. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object govde, HttpHeaders basliklar,
            HttpStatusCode durum, WebRequest istek) {
        if (govde instanceof ProblemDetail problem) {
            if (problem.getProperties() == null || !problem.getProperties().containsKey(KOD_ALANI)) {
                problem.setProperty(KOD_ALANI, durumAdi(durum));
            }
            zenginlestir(problem);
        }
        return super.createResponseEntity(govde, basliklar, durum, istek);
    }

    private ResponseEntity<ProblemDetail> problemCevabi(ProblemDetail problem) {
        zenginlestir(problem);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private static void zenginlestir(ProblemDetail problem) {
        problem.setProperty(KORELASYON_ALANI, MDC.get(KorelasyonKimligi.MDC_ANAHTARI));
        problem.setProperty(ZAMAN_ALANI, Instant.now().toString());
    }

    private static List<AlanHatasi> alanHatalari(MethodArgumentNotValidException istisna) {
        return istisna.getBindingResult().getFieldErrors().stream()
                .map(hata -> new AlanHatasi(hata.getField(), hata.getDefaultMessage()))
                .toList();
    }

    private static String durumAdi(HttpStatusCode durum) {
        HttpStatus bilinenDurum = HttpStatus.resolve(durum.value());
        return bilinenDurum != null ? bilinenDurum.name() : "HTTP_" + durum.value();
    }
}
