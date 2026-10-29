package com.sinema.auth.oturum.service;

import com.sinema.auth.config.KimlikAyarlari;
import com.sinema.auth.kullanici.repository.KullaniciRepository;
import com.sinema.auth.oturum.entity.YenilemeTokeni;
import com.sinema.auth.oturum.repository.YenilemeTokeniRepository;
import com.sinema.common.hata.YetkisizIstisnasi;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Refresh token yaşam döngüsü (planlama §5.2): aile başlatma, rotasyon, yeniden kullanım tespiti, çıkış.
 *
 * Karar tablosu (gelen token için):
 *  - bilinmiyor / süresi dolmuş / çıkışla iptal edilmiş           → 401 INVALID_REFRESH_TOKEN
 *  - geçerli                                                        → yeni token (rotasyon) + access token
 *  - rotasyonla değişmiş, değişim üzerinden ≤ tolerans geçmiş       → yalnızca access token (iki sekme yarışı)
 *  - rotasyonla değişmiş, tolerans geçmiş                           → TÜM AİLE iptal, 401 REFRESH_TOKEN_REUSED
 * Son satırın mantığı: eski bir token'ın tekrar gelmesi, birinin onu kopyaladığını gösterir. Hangisinin
 * gerçek kullanıcı olduğunu bilemeyiz; ikisini de oturumdan atmak en güvenlisidir.
 */
@Service
public class YenilemeServisi {

    public static final String GECERSIZ_YENILEME_KODU = "INVALID_REFRESH_TOKEN";
    public static final String YENIDEN_KULLANIM_KODU = "REFRESH_TOKEN_REUSED";
    private static final String OTURUM_GECERSIZ_MESAJI = "Oturumunuz sona erdi. Lütfen yeniden giriş yapın.";

    private static final Logger log = LoggerFactory.getLogger(YenilemeServisi.class);

    private final YenilemeTokeniRepository tokenRepository;
    private final KullaniciRepository kullaniciRepository;
    private final ErisimTokeniUretici erisimTokeniUretici;
    private final KimlikAyarlari.Yenileme ayarlar;
    private final Clock saat;

    public YenilemeServisi(YenilemeTokeniRepository tokenRepository, KullaniciRepository kullaniciRepository,
            ErisimTokeniUretici erisimTokeniUretici, KimlikAyarlari ayarlar, Clock saat) {
        this.tokenRepository = tokenRepository;
        this.kullaniciRepository = kullaniciRepository;
        this.erisimTokeniUretici = erisimTokeniUretici;
        this.ayarlar = ayarlar.yenileme();
        this.saat = saat;
    }

    /** Girişte çağrılır: yeni bir token ailesi başlatır, ham token değerini döner. */
    @Transactional
    public String yeniAileBaslat(Long kullaniciId) {
        String ham = YenilemeTokeniDegeri.uret();
        tokenKaydet(kullaniciId, ham, UUID.randomUUID().toString(), saat.instant());
        return ham;
    }

    /** noRollbackFor: yeniden kullanımda aile iptali, ardından 401 fırlatılsa da kalıcı olmalı. */
    @Transactional(noRollbackFor = YetkisizIstisnasi.class)
    public OturumBilgisi yenile(String hamToken) {
        YenilemeTokeni token = tokenBul(hamToken);
        Instant simdi = saat.instant();
        if (token.iptalEdilmisMi()) {
            return iptalEdilmisTokeniIsle(token, simdi);
        }
        if (token.suresiDolmusMu(simdi)) {
            throw oturumGecersiz(GECERSIZ_YENILEME_KODU);
        }
        String yeniHam = YenilemeTokeniDegeri.uret();
        YenilemeTokeni yeni = tokenKaydet(token.getKullaniciId(), yeniHam, token.getAileKimligi(), simdi);
        token.yenisiyleDegistir(simdi, yeni.getId());
        return new OturumBilgisi(erisimTokeniUret(token.getKullaniciId()), yeniHam);
    }

    /** Çıkış: token'ın tüm ailesi iptal edilir. Token yok/bilinmiyorsa sessizce geçilir (idempotent). */
    @Transactional
    public void cikisYap(String hamToken) {
        if (!StringUtils.hasText(hamToken)) {
            return;
        }
        tokenRepository.findByTokenOzeti(YenilemeTokeniDegeri.ozet(hamToken))
                .ifPresent(token -> tokenRepository.aileyiIptalEt(token.getAileKimligi(), saat.instant()));
    }

    private OturumBilgisi iptalEdilmisTokeniIsle(YenilemeTokeni token, Instant simdi) {
        if (token.getYerineGecenId() == null) {
            // Rotasyonla değil çıkışla iptal edilmiş: alarm değil, sadece geçersiz
            throw oturumGecersiz(GECERSIZ_YENILEME_KODU);
        }
        boolean toleransIcinde = !simdi.isAfter(token.getIptalZamani().plus(ayarlar.tolerans()));
        if (toleransIcinde) {
            // Diğer sekme az önce yeniledi ve tarayıcı yeni cookie'yi zaten aldı; cookie'ye dokunmadan access token ver
            return new OturumBilgisi(erisimTokeniUret(token.getKullaniciId()), null);
        }
        tokenRepository.aileyiIptalEt(token.getAileKimligi(), simdi);
        log.warn("Refresh token yeniden kullanıldı; token ailesi iptal edildi (kullaniciId={})", token.getKullaniciId());
        throw oturumGecersiz(YENIDEN_KULLANIM_KODU);
    }

    private YenilemeTokeni tokenBul(String hamToken) {
        if (!StringUtils.hasText(hamToken)) {
            throw oturumGecersiz(GECERSIZ_YENILEME_KODU);
        }
        return tokenRepository.findByTokenOzeti(YenilemeTokeniDegeri.ozet(hamToken))
                .orElseThrow(() -> oturumGecersiz(GECERSIZ_YENILEME_KODU));
    }

    private YenilemeTokeni tokenKaydet(Long kullaniciId, String ham, String aile, Instant simdi) {
        return tokenRepository.save(YenilemeTokeni.olustur(kullaniciId, YenilemeTokeniDegeri.ozet(ham), aile, simdi,
                simdi.plus(ayarlar.sure())));
    }

    // Rol her yenilemede DB'den okunur: rol değişikliği en geç bir access token süresinde yansır (planlama §5.4)
    private ErisimTokeni erisimTokeniUret(Long kullaniciId) {
        return kullaniciRepository.findById(kullaniciId)
                .map(erisimTokeniUretici::uret)
                .orElseThrow(() -> oturumGecersiz(GECERSIZ_YENILEME_KODU));
    }

    private static YetkisizIstisnasi oturumGecersiz(String kod) {
        return new YetkisizIstisnasi(kod, OTURUM_GECERSIZ_MESAJI);
    }
}
