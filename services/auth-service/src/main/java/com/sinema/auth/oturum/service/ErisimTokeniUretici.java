package com.sinema.auth.oturum.service;

import com.sinema.auth.config.KimlikAyarlari;
import com.sinema.auth.kullanici.entity.Kullanici;
import com.sinema.common.guvenlik.GuvenlikAyarlari;
import com.sinema.common.guvenlik.Roller;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * RS256 imzalı access token üretir (planlama §5.1, §5.5). İki tür:
 *  - kullanıcı: sub = kullanıcı id, role = USER/ADMIN, 15 dk
 *  - servis   : sub = servis adı, role = SERVICE, aud = hedef servis, 5 dk
 * İçerik bilerek az; e-posta gibi kişisel veri yok: JWT imzalıdır ama şifreli değildir, herkes içini okuyabilir.
 */
@Component
@RequiredArgsConstructor
public class ErisimTokeniUretici {

    private final JwtEncoder jwtEncoder;
    private final KimlikAyarlari kimlikAyarlari;
    private final GuvenlikAyarlari guvenlikAyarlari;
    private final Clock saat;

    public ErisimTokeni uret(Kullanici kullanici) {
        return imzala(kimlikAyarlari.jwt().erisimTokenSuresi(), icerik -> icerik
                .subject(kullanici.getId().toString())
                .claim(Roller.ROL_ALANI, kullanici.getRol().name()));
    }

    public ErisimTokeni servisTokeniUret(String cagiranServis, String hedefServis) {
        return imzala(kimlikAyarlari.jwt().servisTokenSuresi(), icerik -> icerik
                .subject(cagiranServis)
                .claim(Roller.ROL_ALANI, Roller.SERVICE)
                .audience(List.of(hedefServis)));
    }

    // Ortak alanlar (yayıncı, zamanlar, benzersiz id) tek yerde; tür farkı yalnızca 'ozellestir' içinde
    private ErisimTokeni imzala(Duration gecerlilik, Consumer<JwtClaimsSet.Builder> ozellestir) {
        Instant simdi = saat.instant();
        JwtClaimsSet.Builder icerik = JwtClaimsSet.builder()
                .issuer(guvenlikAyarlari.jwt().yayinci())
                .issuedAt(simdi)
                .expiresAt(simdi.plus(gecerlilik))
                .id(UUID.randomUUID().toString());
        ozellestir.accept(icerik);
        JwsHeader baslik = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String deger = jwtEncoder.encode(JwtEncoderParameters.from(baslik, icerik.build())).getTokenValue();
        return new ErisimTokeni(deger, gecerlilik);
    }
}
