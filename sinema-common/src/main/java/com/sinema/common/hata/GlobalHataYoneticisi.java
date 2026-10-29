package com.sinema.common.hata;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public static final String ALAN_HATALARI = "errors";

    public static final String DOGRULAMA_HATASI_KODU = "VALIDATION_FAILED";
    public static final String BEKLENMEYEN_HATA_KODU = "INTERNAL_ERROR";

    private static final Logger log = LoggerFactory.getLogger(GlobalHataYoneticisi.class);

    @ExceptionHandler(SinemaIstisnasi.class)
    public ResponseEntity<ProblemDetail> sinemaIstisnasiniIsle(SinemaIstisnasi istisna) {
        ProblemDetail problem = ProblemDetaylari.olustur(istisna.getDurum(), istisna.getKod(), istisna.getMessage());
        istisna.getEkAlanlar().forEach(problem::setProperty);
        return ResponseEntity.status(istisna.getDurum()).headers(istisna.getBasliklar()).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> beklenmeyenHatayiIsle(Exception istisna) {
        // Ayrıntı yalnızca loga gider (korelasyon kimliğiyle); istemci genel bir mesaj görür
        log.error("Beklenmeyen hata", istisna);
        ProblemDetail problem = ProblemDetaylari.olustur(HttpStatus.INTERNAL_SERVER_ERROR, BEKLENMEYEN_HATA_KODU,
                "Beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException istisna,
            HttpHeaders basliklar, HttpStatusCode durum, WebRequest istek) {
        ProblemDetail problem = istisna.getBody();
        problem.setDetail("İstekte geçersiz alanlar var.");
        problem.setProperty(ProblemDetaylari.KOD_ALANI, DOGRULAMA_HATASI_KODU);
        problem.setProperty(ALAN_HATALARI, alanHatalari(istisna));
        return handleExceptionInternal(istisna, problem, basliklar, durum, istek);
    }

    /** Üst sınıfın ürettiği tüm cevaplar buradan geçer: kodu olmayanlara HTTP durum adı kod olarak verilir. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object govde, HttpHeaders basliklar,
            HttpStatusCode durum, WebRequest istek) {
        if (govde instanceof ProblemDetail problem) {
            if (problem.getProperties() == null || !problem.getProperties().containsKey(ProblemDetaylari.KOD_ALANI)) {
                problem.setProperty(ProblemDetaylari.KOD_ALANI, durumAdi(durum));
            }
            ProblemDetaylari.zenginlestir(problem);
        }
        return super.createResponseEntity(govde, basliklar, durum, istek);
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
