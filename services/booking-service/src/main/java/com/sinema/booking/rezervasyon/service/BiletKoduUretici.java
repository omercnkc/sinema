package com.sinema.booking.rezervasyon.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Bilet (rezervasyon) kodu üretir (K30): QR doğrulama adresi /bilet/{kod} bu koddur, herkese açık uçta sorgulanır.
 * Bu yüzden tahmin edilemez olmalı: SecureRandom, 16 karakter × 5 bit = 80 bit rastgelelik (gateway hız sınırıyla
 * birlikte deneme yoluyla bulunamaz). Alfabe Crockford Base32: karışan karakterler (I, L, O, U) yok, biletten
 * okunup elle yazılabilir. Çakışma olasılığı ihmal edilebilir; yine de DB'de UNIQUE.
 */
@Component
public class BiletKoduUretici {

    static final int UZUNLUK = 16;
    private static final char[] ALFABE = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private final SecureRandom rastgele = new SecureRandom();

    public String uret() {
        char[] kod = new char[UZUNLUK];
        for (int i = 0; i < UZUNLUK; i++) {
            kod[i] = ALFABE[rastgele.nextInt(ALFABE.length)];
        }
        return new String(kod);
    }
}
