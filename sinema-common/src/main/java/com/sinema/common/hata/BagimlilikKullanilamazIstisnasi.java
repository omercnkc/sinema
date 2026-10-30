package com.sinema.common.hata;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * 503: isteği karşılamak için gereken dış servis (TMDB, iyzico) veya başka bir iç servis şu an yanıt vermiyor.
 * Hata geçicidir; istemci sonra tekrar deneyebilir (planlama §7.2, fail fast).
 */
public class BagimlilikKullanilamazIstisnasi extends SinemaIstisnasi {

    public BagimlilikKullanilamazIstisnasi(String kod, String mesaj) {
        super(kod, mesaj, Map.of());
    }

    @Override
    public HttpStatus getDurum() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}
