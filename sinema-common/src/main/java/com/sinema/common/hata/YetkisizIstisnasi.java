package com.sinema.common.hata;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** 401: kimlik doğrulanamadı (ör. hatalı e-posta/parola). Mesaj hangi bilginin yanlış olduğunu söylemez. */
public class YetkisizIstisnasi extends SinemaIstisnasi {

    public YetkisizIstisnasi(String kod, String mesaj) {
        super(kod, mesaj, Map.of());
    }

    @Override
    public HttpStatus getDurum() {
        return HttpStatus.UNAUTHORIZED;
    }
}
