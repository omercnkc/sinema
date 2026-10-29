package com.sinema.auth.kullanici.service;

import com.sinema.auth.kullanici.dto.ProfilCevabi;
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

    @Transactional(readOnly = true)
    public ProfilCevabi profil(Long kullaniciId) {
        return kullaniciRepository.findById(kullaniciId)
                .map(kullanici -> new ProfilCevabi(kullanici.getId(), kullanici.getEmail(), kullanici.getRol().name()))
                .orElseThrow(() -> new KaynakBulunamadiIstisnasi(KULLANICI_BULUNAMADI_KODU, "Kullanıcı bulunamadı."));
    }
}
