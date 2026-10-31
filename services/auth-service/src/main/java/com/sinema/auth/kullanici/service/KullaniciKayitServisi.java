package com.sinema.auth.kullanici.service;

import com.sinema.auth.kullanici.dto.AdminIstegi;
import com.sinema.auth.kullanici.dto.KayitCevabi;
import com.sinema.auth.kullanici.dto.KayitIstegi;
import com.sinema.auth.kullanici.dto.TelefonNumarasi;
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

    /**
     * Herkese açık kayıt: rol her zaman USER (admin bu yoldan oluşturulamaz, planlama §5.6). Ad/soyad baştaki ve
     * sondaki boşluktan arındırılır, telefon +905XXXXXXXXX biçimine çevrilir (K48).
     */
    @Transactional
    public KayitCevabi kaydet(KayitIstegi istek) {
        String email = kayitliDegilseNormalizeEt(istek.email());
        String telefon = TelefonNumarasi.normalizeEt(istek.phone())
                .orElseThrow(() -> new IllegalArgumentException("Telefon doğrulanmış olmalı")); // @GecerliTelefon
        Kullanici kullanici = Kullanici.kisiBilgileriyleOlustur(email, parolaKodlayici.encode(istek.password()),
                Rol.USER, istek.firstName().trim(), istek.lastName().trim(), telefon);
        return new KayitCevabi(kaydetVeyaCakismaBildir(kullanici).getId(), email);
    }

    /** Yalnızca mevcut bir admin (veya açılıştaki ilk admin kurulumu) çağırır; yetki kontrolü çağıranda. */
    @Transactional
    public KayitCevabi adminEkle(AdminIstegi istek) {
        String email = kayitliDegilseNormalizeEt(istek.email());
        Kullanici kullanici = Kullanici.olustur(email, parolaKodlayici.encode(istek.password()), Rol.ADMIN);
        return new KayitCevabi(kaydetVeyaCakismaBildir(kullanici).getId(), email);
    }

    private String kayitliDegilseNormalizeEt(String girilenEmail) {
        String email = emailNormalizeEt(girilenEmail);
        if (kullaniciRepository.existsByEmail(email)) {
            throw new CakismaIstisnasi(EMAIL_KAYITLI_KODU, EMAIL_KAYITLI_MESAJI);
        }
        return email;
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
