package com.sinema.common.hata;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * 429: istemci bir süre beklemeli (hesap kilidi, K29 hold bekleme süresi...).
 * Bekleme süresi hem Retry-After başlığına hem gövdeye (retryAfterSeconds) yazılır.
 */
public class CokFazlaIstekIstisnasi extends SinemaIstisnasi {

    public static final String BEKLEME_ALANI = "retryAfterSeconds";

    private final long beklemeSaniyesi;

    public CokFazlaIstekIstisnasi(String kod, String mesaj, Duration bekleme) {
        super(kod, mesaj, Map.of(BEKLEME_ALANI, yukariYuvarlaSaniye(bekleme)));
        this.beklemeSaniyesi = yukariYuvarlaSaniye(bekleme);
    }

    @Override
    public HttpStatus getDurum() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }

    @Override
    public HttpHeaders getBasliklar() {
        HttpHeaders basliklar = new HttpHeaders();
        basliklar.set(HttpHeaders.RETRY_AFTER, String.valueOf(beklemeSaniyesi));
        return basliklar;
    }

    // Aşağı yuvarlansaydı istemci kilit bitmeden tekrar deneyip yine reddedilirdi; en az 1 sn
    private static long yukariYuvarlaSaniye(Duration bekleme) {
        long saniye = bekleme.toSeconds() + (bekleme.toNanosPart() > 0 ? 1 : 0);
        return Math.max(1, saniye);
    }
}
