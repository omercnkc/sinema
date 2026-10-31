package com.sinema.payment.saglayici.iyzico;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * iyzico imzaları (planlama §9.3-3, sandbox'ta doğrulandı).
 *
 * İstek (IYZWSv2): imza = hex(HMAC-SHA256(gizliAnahtar, rastgeleAnahtar + yol + gövde)); başlık
 * "IYZWSv2 " + base64("apiKey:..&randomKey:..&signature:.."). Gövde, gönderilen baytlarla birebir aynı olmalı.
 *
 * Cevap: hex(HMAC-SHA256(gizliAnahtar, alanlar ":" ile birleşik)); fiyatlarda sondaki sıfırlar atılır ("300.00" → "300").
 * Karşılaştırma sabit sürede yapılır.
 */
public class IyzicoImzalayici {

    private static final String HMAC = "HmacSHA256";
    private static final String ON_EK = "IYZWSv2 ";
    private static final String AYRAC = ":";

    private final String apiAnahtari;
    private final SecretKeySpec gizliAnahtar;
    private final Supplier<String> rastgeleAnahtar;

    public IyzicoImzalayici(String apiAnahtari, String gizliAnahtar, Supplier<String> rastgeleAnahtar) {
        this.apiAnahtari = apiAnahtari;
        this.gizliAnahtar = new SecretKeySpec(gizliAnahtar.getBytes(StandardCharsets.UTF_8), HMAC);
        this.rastgeleAnahtar = rastgeleAnahtar;
    }

    /** Bir istek için imzalı başlıklar; rastgele anahtar aynı değerle x-iyzi-rnd başlığına da yazılır. */
    public ImzaliBasliklar istekImzala(String yol, byte[] govde) {
        String rastgele = rastgeleAnahtar.get();
        String imza = hmacHex(rastgele + yol + new String(govde, StandardCharsets.UTF_8));
        String yetki = "apiKey:" + apiAnahtari + "&randomKey:" + rastgele + "&signature:" + imza;
        return new ImzaliBasliklar(ON_EK + Base64.getEncoder().encodeToString(yetki.getBytes(StandardCharsets.UTF_8)),
                rastgele);
    }

    /** Cevaptaki "signature" alanı, verilen alanların (sırası iyzico'nun belirlediği gibi) imzasıyla eşleşiyor mu. */
    public boolean cevapImzasiDogruMu(List<String> alanlar, String beklenenImza) {
        if (beklenenImza == null) {
            return false;
        }
        byte[] hesaplanan = hmacHex(String.join(AYRAC, alanlar)).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(hesaplanan, beklenenImza.getBytes(StandardCharsets.UTF_8));
    }

    /** iyzico kuralı: imzada fiyatın sondaki sıfırları atılır (10.50 → 10.5, 300.00 → 300). */
    public static String fiyatNormalizeEt(BigDecimal fiyat) {
        return fiyat == null ? "" : fiyat.stripTrailingZeros().toPlainString();
    }

    private String hmacHex(String veri) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(gizliAnahtar);
            return HexFormat.of().formatHex(mac.doFinal(veri.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 kullanılamıyor", e);
        }
    }

    /** yetki: Authorization başlığının değeri; rastgeleAnahtar: x-iyzi-rnd başlığının değeri. */
    public record ImzaliBasliklar(String yetki, String rastgeleAnahtar) {
    }
}
