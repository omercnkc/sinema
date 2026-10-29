package com.sinema.auth.oturum.service;

import com.sinema.auth.config.KimlikAyarlari;
import com.sinema.auth.kullanici.entity.Kullanici;
import com.sinema.auth.kullanici.repository.KullaniciRepository;
import com.sinema.auth.kullanici.service.KullaniciKayitServisi;
import com.sinema.auth.oturum.dto.GirisCevabi;
import com.sinema.auth.oturum.dto.GirisIstegi;
import com.sinema.common.hata.CokFazlaIstekIstisnasi;
import com.sinema.common.hata.YetkisizIstisnasi;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * E-posta + parola ile giriş ve hesap kilidi (planlama §5.3, §5.6, K36).
 *
 * Bilerek @Transactional DEĞİL: başarısız deneme sayacı, ardından 401 fırlatılsa bile kalıcı olmalı.
 * Metot transaction içinde olsaydı istisna sayaç güncellemesini de geri alırdı ve kilit hiç devreye girmezdi.
 * Sayaç güncellemeleri repository'de kendi transaction'larında çalışır.
 */
@Service
public class GirisServisi {

    public static final String GECERSIZ_KIMLIK_BILGISI_KODU = "INVALID_CREDENTIALS";
    public static final String HESAP_KILITLI_KODU = "ACCOUNT_LOCKED";
    private static final String GECERSIZ_KIMLIK_BILGISI_MESAJI = "E-posta veya parola hatalı.";
    private static final String HESAP_KILITLI_MESAJI = "Çok fazla hatalı deneme yapıldı. Lütfen daha sonra tekrar deneyin.";

    private final KullaniciRepository kullaniciRepository;
    private final PasswordEncoder parolaKodlayici;
    private final ErisimTokeniUretici erisimTokeniUretici;
    private final KimlikAyarlari.Kilit kilitAyarlari;
    private final Clock saat;
    // Var olmayan e-postada da bir BCrypt karşılaştırması yapmak için (aşağıya bakın)
    private final String sahteParolaOzeti;

    public GirisServisi(KullaniciRepository kullaniciRepository, PasswordEncoder parolaKodlayici,
            ErisimTokeniUretici erisimTokeniUretici, KimlikAyarlari ayarlar, Clock saat) {
        this.kullaniciRepository = kullaniciRepository;
        this.parolaKodlayici = parolaKodlayici;
        this.erisimTokeniUretici = erisimTokeniUretici;
        this.kilitAyarlari = ayarlar.kilit();
        this.saat = saat;
        this.sahteParolaOzeti = parolaKodlayici.encode(UUID.randomUUID().toString());
    }

    public GirisCevabi girisYap(GirisIstegi istek) {
        Optional<Kullanici> bulunan = kullaniciRepository.findByEmail(
                KullaniciKayitServisi.emailNormalizeEt(istek.email()));
        if (bulunan.isEmpty()) {
            // Kullanıcı yokken hemen dönülseydi cevap ~250 ms daha hızlı gelir ve e-postanın kayıtlı
            // olup olmadığı süreden anlaşılırdı. Aynı maliyette sahte bir karşılaştırma bu farkı kapatır.
            parolaKodlayici.matches(istek.password(), sahteParolaOzeti);
            throw gecersizKimlikBilgisi();
        }
        Kullanici kullanici = bulunan.get();
        Instant simdi = saat.instant();
        kilitliyseReddet(kullanici, simdi);

        if (!parolaKodlayici.matches(istek.password(), kullanici.getParolaOzeti())) {
            kullaniciRepository.basarisizGirisiKaydet(kullanici.getId(), kilitAyarlari.esik(),
                    simdi.plus(kilitAyarlari.sure()));
            throw gecersizKimlikBilgisi();
        }
        sayaclarDoluysaSifirla(kullanici);
        ErisimTokeni token = erisimTokeniUretici.uret(kullanici);
        return new GirisCevabi(token.deger(), token.gecerlilik().toSeconds());
    }

    // Kilitliyken parola hiç denetlenmez: doğru parola da kilidi kaldırmaz, süre dolmalıdır
    private void kilitliyseReddet(Kullanici kullanici, Instant simdi) {
        Instant kilitBitisi = kullanici.getKilitBitisi();
        if (kilitBitisi != null && kilitBitisi.isAfter(simdi)) {
            throw new CokFazlaIstekIstisnasi(HESAP_KILITLI_KODU, HESAP_KILITLI_MESAJI,
                    Duration.between(simdi, kilitBitisi));
        }
    }

    // Her başarılı girişte gereksiz UPDATE olmasın: yalnızca sayaç veya eski kilit varsa yazılır
    private void sayaclarDoluysaSifirla(Kullanici kullanici) {
        if (kullanici.getBasarisizGirisSayisi() > 0 || kullanici.getKilitBitisi() != null) {
            kullaniciRepository.girisSayaclariniSifirla(kullanici.getId());
        }
    }

    private static YetkisizIstisnasi gecersizKimlikBilgisi() {
        return new YetkisizIstisnasi(GECERSIZ_KIMLIK_BILGISI_KODU, GECERSIZ_KIMLIK_BILGISI_MESAJI);
    }
}
