package com.sinema.auth.servis.service;

import com.sinema.auth.config.KimlikAyarlari;
import com.sinema.auth.oturum.service.ErisimTokeni;
import com.sinema.auth.oturum.service.ErisimTokeniUretici;
import com.sinema.auth.servis.dto.ServisTokeniCevabi;
import com.sinema.auth.servis.dto.ServisTokeniIstegi;
import com.sinema.common.hata.ErisimReddiIstisnasi;
import com.sinema.common.hata.YetkisizIstisnasi;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Servisler arası kimlik (planlama §5.5, K36): bir servis clientId + clientSecret ile kendini tanıtır,
 * belirli bir hedef servis için kısa ömürlü (5 dk) SERVICE token'ı alır.
 * Hangi servisin hangi hedefe token alabileceği burada sınırlanır (ör. yalnızca payment → booking).
 */
@Service
@RequiredArgsConstructor
public class ServisTokeniServisi {

    public static final String GECERSIZ_ISTEMCI_KODU = "INVALID_CLIENT";
    public static final String IZINSIZ_HEDEF_KODU = "AUDIENCE_NOT_ALLOWED";

    private final KimlikAyarlari ayarlar;
    private final ErisimTokeniUretici erisimTokeniUretici;

    public ServisTokeniCevabi tokenVer(ServisTokeniIstegi istek) {
        KimlikAyarlari.ServisIstemcisi istemci = ayarlar.servisIstemcileri().get(istek.clientId());
        if (istemci == null || !StringUtils.hasText(istemci.sir()) || !sirEslesiyor(istemci.sir(), istek.clientSecret())) {
            throw new YetkisizIstisnasi(GECERSIZ_ISTEMCI_KODU, "Servis kimlik bilgileri geçersiz.");
        }
        if (!istemci.izinliHedefler().contains(istek.audience())) {
            throw new ErisimReddiIstisnasi(IZINSIZ_HEDEF_KODU, "Bu servis istenen hedef için token alamaz.");
        }
        ErisimTokeni token = erisimTokeniUretici.servisTokeniUret(istek.clientId(), istek.audience());
        return new ServisTokeniCevabi(token.deger(), token.gecerlilik().toSeconds());
    }

    /**
     * Sabit sürede karşılaştırma: String.equals ilk farklı karakterde durur; cevap süresinden sırrın
     * kaç karakterinin doğru olduğu ölçülebilirdi. MessageDigest.isEqual her zaman tüm baytlara bakar.
     */
    private static boolean sirEslesiyor(String beklenen, String gelen) {
        return MessageDigest.isEqual(beklenen.getBytes(StandardCharsets.UTF_8), gelen.getBytes(StandardCharsets.UTF_8));
    }
}
