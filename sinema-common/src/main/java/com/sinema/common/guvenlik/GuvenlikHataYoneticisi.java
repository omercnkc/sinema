package com.sinema.common.guvenlik;

import com.sinema.common.hata.ProblemDetaylari;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @PreAuthorize reddi controller metodu çağrılırken oluşur ve MVC'nin hata zincirine düşer.
 * Bu sınıf olmasa GlobalHataYoneticisi'ndeki genel Exception yakalayıcısı onu 500'e çevirirdi.
 * Ayrı sınıf, çünkü yalnızca Spring Security olan serviste yüklenebilir; önceliği yüksek tutulur ki önce o denensin.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GuvenlikHataYoneticisi {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> erisimReddiniIsle(AccessDeniedException istisna) {
        ProblemDetail problem = ProblemDetaylari.olustur(HttpStatus.FORBIDDEN, ProblemJsonErisimReddiIsleyicisi.KOD,
                ProblemJsonErisimReddiIsleyicisi.MESAJ);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }
}
