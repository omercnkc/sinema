package com.sinema.auth.kullanici.service;

import com.sinema.auth.kullanici.dto.KayitCevabi;
import com.sinema.auth.kullanici.dto.KayitIstegi;
import com.sinema.auth.kullanici.entity.Kullanici;
import com.sinema.auth.kullanici.entity.Rol;
import com.sinema.auth.kullanici.repository.KullaniciRepository;
import com.sinema.common.hata.CakismaIstisnasi;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KullaniciKayitServisi {

    public static final String EMAIL_KAYITLI_KODU = "EMAIL_ALREADY_REGISTERED";
    private static final String EMAIL_KAYITLI_MESAJI = "Bu e-posta adresiyle kayıtlı bir hesap var.";

    private final KullaniciRepository kullaniciRepository;
    private final PasswordEncoder parolaKodlayici;

    /** Herkese açık kayıt: rol her zaman USER (admin bu yoldan oluşturulamaz, planlama §5.6). */
    @Transactional
    public KayitCevabi kaydet(KayitIstegi istek) {
        return rolIleKaydet(istek, Rol.USER);
    }

    /** Yalnızca mevcut bir admin (veya açılıştaki ilk admin kurulumu) çağırır; yetki kontrolü çağıranda. */
    @Transactional
    public KayitCevabi adminEkle(KayitIstegi istek) {
        return rolIleKaydet(istek, Rol.ADMIN);
    }

    private KayitCevabi rolIleKaydet(KayitIstegi istek, Rol rol) {
        String email = emailNormalizeEt(istek.email());
        if (kullaniciRepository.existsByEmail(email)) {
            throw new CakismaIstisnasi(EMAIL_KAYITLI_KODU, EMAIL_KAYITLI_MESAJI);
        }
        Kullanici kullanici = Kullanici.olustur(email, parolaKodlayici.encode(istek.password()), rol);
        return new KayitCevabi(kaydetVeyaCakismaBildir(kullanici).getId(), email);
    }

    /**
     * Aynı e-postayla iki istek aynı anda gelirse ikisi de existsByEmail'i geçebilir;
     * asıl güvence DB'deki UNIQUE kısıtıdır. saveAndFlush, ihlali burada (transaction içinde) yakalatır.
     */
    private Kullanici kaydetVeyaCakismaBildir(Kullanici kullanici) {
        try {
            return kullaniciRepository.saveAndFlush(kullanici);
        } catch (DataIntegrityViolationException e) {
            throw new CakismaIstisnasi(EMAIL_KAYITLI_KODU, EMAIL_KAYITLI_MESAJI);
        }
    }

    /**
     * Locale.ROOT bilinçli: Türkçe locale'de "I".toLowerCase() noktasız "ı" üretir,
     * böylece "INFO@x.com" ile "info@x.com" iki ayrı hesap olurdu.
     */
    public static String emailNormalizeEt(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
