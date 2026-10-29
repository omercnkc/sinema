package com.sinema.auth.oturum.service;

import com.sinema.auth.config.KimlikAyarlari;
import com.sinema.auth.kullanici.entity.Kullanici;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Kullanıcı için RS256 imzalı access token üretir (planlama §5.1).
 * İçerik bilerek az: kimlik (sub), rol, yayıncı, zamanlar ve benzersiz id. E-posta gibi kişisel veri yok;
 * JWT imzalıdır ama şifreli değildir, herkes içini okuyabilir.
 */
@Component
@RequiredArgsConstructor
public class ErisimTokeniUretici {

    public static final String ROL_ALANI = "role";

    private final JwtEncoder jwtEncoder;
    private final KimlikAyarlari ayarlar;
    private final Clock saat;

    public ErisimTokeni uret(Kullanici kullanici) {
        Instant simdi = saat.instant();
        Duration gecerlilik = ayarlar.jwt().erisimTokenSuresi();
        JwtClaimsSet icerik = JwtClaimsSet.builder()
                .issuer(ayarlar.jwt().yayinci())
                .subject(kullanici.getId().toString())
                .issuedAt(simdi)
                .expiresAt(simdi.plus(gecerlilik))
                .id(UUID.randomUUID().toString())
                .claim(ROL_ALANI, kullanici.getRol().name())
                .build();
        JwsHeader baslik = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String deger = jwtEncoder.encode(JwtEncoderParameters.from(baslik, icerik)).getTokenValue();
        return new ErisimTokeni(deger, gecerlilik);
    }
}
