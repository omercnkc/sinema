package com.sinema.auth.kullanici.service;

import com.sinema.auth.kullanici.dto.AliciBilgisi;
import com.sinema.auth.kullanici.dto.ProfilCevabi;
import com.sinema.auth.kullanici.entity.Kullanici;
import com.sinema.auth.kullanici.repository.KullaniciRepository;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfilServisi {

    public static final String KULLANICI_BULUNAMADI_KODU = "USER_NOT_FOUND";

    private final KullaniciRepository kullaniciRepository;

    /** GET /me: yalnızca token sahibinin kendi bilgileri. */
    @Transactional(readOnly = true)
    public ProfilCevabi profil(Long kullaniciId) {
        Kullanici kullanici = bul(kullaniciId);
        return new ProfilCevabi(kullanici.getId(), kullanici.getEmail(), kullanici.getRol().name(),
                kullanici.getAd(), kullanici.getSoyad(), kullanici.getTelefon());
    }

    /** payment-service'in iyzico alıcı bilgisi (iç uç, K48). */
    @Transactional(readOnly = true)
    public AliciBilgisi aliciBilgisi(Long kullaniciId) {
        Kullanici kullanici = bul(kullaniciId);
        return new AliciBilgisi(kullanici.getId(), kullanici.getEmail(), kullanici.getAd(), kullanici.getSoyad(),
                kullanici.getTelefon());
    }

    private Kullanici bul(Long kullaniciId) {
        return kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new KaynakBulunamadiIstisnasi(KULLANICI_BULUNAMADI_KODU, "Kullanıcı bulunamadı."));
    }
}
