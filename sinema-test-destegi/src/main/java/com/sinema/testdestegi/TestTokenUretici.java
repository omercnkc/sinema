package com.sinema.testdestegi;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Servis testlerinin auth-service'e gitmeden token üretmesi için: test anahtarıyla, auth'un ürettiği
 * biçimde (iss, sub, role, aud, exp...) imzalar. Bozuk token senaryoları (süresi dolmuş, yanlış yayıncı,
 * yabancı anahtar) da buradan üretilir.
 */
public final class TestTokenUretici {

    public static final String YAYINCI = "sinema-auth";

    private static final JwtEncoder TEST_ANAHTARIYLA = encoder(TestJwtAnahtarlari.acikAnahtar(),
            TestJwtAnahtarlari.ozelAnahtar());

    private TestTokenUretici() {
    }

    public static String kullaniciTokeni(long kullaniciId, String rol) {
        return imzala(icerik -> icerik.subject(String.valueOf(kullaniciId)).claim("role", rol));
    }

    public static String servisTokeni(String cagiranServis, String hedefServis) {
        return imzala(icerik -> icerik.subject(cagiranServis).claim("role", "SERVICE").audience(List.of(hedefServis)));
    }

    public static String suresiDolmusKullaniciTokeni() {
        Instant gecmis = Instant.now().minus(Duration.ofHours(1));
        return imzala(icerik -> icerik.subject("1").claim("role", "USER")
                .issuedAt(gecmis).expiresAt(gecmis.plus(Duration.ofMinutes(15))));
    }

    public static String yanlisYayinciliKullaniciTokeni() {
        return imzala(icerik -> icerik.subject("1").claim("role", "USER").issuer("sahte-yayinci"));
    }

    /** Doğru biçimde ama auth'un değil, başka birinin anahtarıyla imzalanmış: sahte token. */
    public static String yabanciAnahtarlaImzaliToken() {
        return encode(yabanciEncoder(), varsayilanIcerik().subject("1").claim("role", "ADMIN").build());
    }

    /** Varsayılan içerik (iss, iat, exp +15 dk, jti) üzerine testin istediği değişiklikler uygulanır. */
    public static String imzala(Consumer<JwtClaimsSet.Builder> ozellestir) {
        JwtClaimsSet.Builder icerik = varsayilanIcerik();
        ozellestir.accept(icerik);
        return encode(TEST_ANAHTARIYLA, icerik.build());
    }

    private static JwtClaimsSet.Builder varsayilanIcerik() {
        Instant simdi = Instant.now();
        return JwtClaimsSet.builder()
                .issuer(YAYINCI)
                .issuedAt(simdi)
                .expiresAt(simdi.plus(Duration.ofMinutes(15)))
                .id(UUID.randomUUID().toString());
    }

    private static String encode(JwtEncoder encoder, JwtClaimsSet icerik) {
        JwsHeader baslik = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(baslik, icerik)).getTokenValue();
    }

    private static JwtEncoder encoder(RSAPublicKey acik, RSAPrivateKey ozel) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(new RSAKey.Builder(acik).privateKey(ozel).build())));
    }

    private static JwtEncoder yabanciEncoder() {
        try {
            KeyPairGenerator uretici = KeyPairGenerator.getInstance("RSA");
            uretici.initialize(2048);
            var cift = uretici.generateKeyPair();
            return encoder((RSAPublicKey) cift.getPublic(), (RSAPrivateKey) cift.getPrivate());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
