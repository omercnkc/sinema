package com.sinema.common.hata;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** 409: istek mevcut durumla çakışıyor (koltuk dolu, e-posta kayıtlı, seans çakışması...). */
public class CakismaIstisnasi extends SinemaIstisnasi {

    public CakismaIstisnasi(String kod, String mesaj) {
        super(kod, mesaj, Map.of());
    }

    public CakismaIstisnasi(String kod, String mesaj, Map<String, Object> ekAlanlar) {
        super(kod, mesaj, ekAlanlar);
    }

    @Override
    public HttpStatus getDurum() {
        return HttpStatus.CONFLICT;
    }
}
