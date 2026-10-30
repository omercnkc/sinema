package com.sinema.catalog.film.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * Türkiye'de film için kullanılan içerik işaretleri (K21, K46). Yaş sınırı ayrı alandadır.
 * TMDB, TR vizyon kaydının "descriptors" alanında bunları İngilizce adla verir (gerçek API ile doğrulandı).
 */
public enum AkilliIsaret {
    SIDDET_KORKU("Violence / Horror"),
    CINSELLIK("Sexuality"),
    OLUMSUZ_ORNEK("Negative examples");

    private final String tmdbDegeri;

    AkilliIsaret(String tmdbDegeri) {
        this.tmdbDegeri = tmdbDegeri;
    }

    /** Bilinmeyen değer boş döner; çağıran yok sayar (admin listeden elle seçer). */
    public static Optional<AkilliIsaret> tmdbDegerinden(String deger) {
        return Arrays.stream(values()).filter(isaret -> isaret.tmdbDegeri.equalsIgnoreCase(deger)).findFirst();
    }
}
