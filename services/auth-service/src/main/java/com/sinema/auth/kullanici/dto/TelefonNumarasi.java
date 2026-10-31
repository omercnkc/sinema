package com.sinema.auth.kullanici.dto;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TR cep telefonu kuralı tek yerde (K48): kullanıcının yazdığı biçimler kabul edilir ("0532 123 45 67",
 * "5321234567", "+90 532 123 4567", "(0532) 123-45-67") ve hepsi +905XXXXXXXXX biçimine çevrilir. Saklanan ve
 * iyzico'ya giden biçim hep aynıdır (iyzico gsmNumber, sandbox'ta doğrulandı).
 */
public final class TelefonNumarasi {

    // Kullanıcının araya koyabileceği ayraçlar
    private static final Pattern AYRACLAR = Pattern.compile("[\\s\\-().]");
    // Önek isteğe bağlı (+90, 0090, 90, 0), ardından 5 ile başlayan 10 hane (TR cep)
    private static final Pattern TR_CEP = Pattern.compile("^(?:\\+90|0090|90|0)?(5\\d{9})$");
    private static final String ULKE_KODU = "+90";

    private TelefonNumarasi() {
    }

    /** Geçerli bir TR cep numarasıysa +905XXXXXXXXX biçimi, değilse boş. */
    public static Optional<String> normalizeEt(String girilen) {
        if (girilen == null) {
            return Optional.empty();
        }
        Matcher eslesme = TR_CEP.matcher(AYRACLAR.matcher(girilen.trim()).replaceAll(""));
        return eslesme.matches() ? Optional.of(ULKE_KODU + eslesme.group(1)) : Optional.empty();
    }
}
