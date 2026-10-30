package com.sinema.common.hata;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * 400: istek, Bean Validation'ın tek başına göremediği bir nedenle geçersiz (ör. seçilen hücre bu seansın koltuğu
 * değil, koltuk sınırı yapılandırmadan okunuyor). Alan biçimi hataları için Bean Validation (VALIDATION_FAILED) kalır.
 */
public class GecersizIstekIstisnasi extends SinemaIstisnasi {

    public GecersizIstekIstisnasi(String kod, String mesaj) {
        super(kod, mesaj, Map.of());
    }

    public GecersizIstekIstisnasi(String kod, String mesaj, Map<String, Object> ekAlanlar) {
        super(kod, mesaj, ekAlanlar);
    }

    @Override
    public HttpStatus getDurum() {
        return HttpStatus.BAD_REQUEST;
    }
}
