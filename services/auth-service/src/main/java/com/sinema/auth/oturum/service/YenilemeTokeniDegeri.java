package com.sinema.auth.oturum.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh token değeri JWT değil, opak rastgele bir dizedir: içinde bilgi yoktur, yalnızca DB'deki kayda işaret eder.
 * 256 bit rastgelelik tahmin edilmesini imkânsız kılar; DB'de yalnızca SHA-256 özeti tutulur.
 * (Parolanın aksine BCrypt gerekmez: değer zaten yüksek entropili, kaba kuvvetle bulunamaz.)
 */
final class YenilemeTokeniDegeri {

    private static final int BAYT_SAYISI = 32;
    private static final SecureRandom RASTGELE = new SecureRandom();

    private YenilemeTokeniDegeri() {
    }

    static String uret() {
        byte[] baytlar = new byte[BAYT_SAYISI];
        RASTGELE.nextBytes(baytlar);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(baytlar);
    }

    static String ozet(String deger) {
        try {
            byte[] ozet = MessageDigest.getInstance("SHA-256").digest(deger.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(ozet);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 desteklenmiyor", e);
        }
    }
}
