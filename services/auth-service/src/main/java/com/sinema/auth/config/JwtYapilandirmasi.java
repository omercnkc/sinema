package com.sinema.auth.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * RS256 anahtarlarını dosyadan yükler; token imzalayan (encoder) ve doğrulayan (decoder) bean'leri kurar.
 * Anahtar dosyası yoksa servis açılmaz (fail fast) ve hata mesajı nasıl üretileceğini söyler.
 */
@Configuration
public class JwtYapilandirmasi {

    @Bean
    public RSAPublicKey jwtAcikAnahtar(KimlikAyarlari ayarlar) {
        return anahtarOku(ayarlar.jwt().acikAnahtar(), RsaKeyConverters.x509());
    }

    @Bean
    public RSAPrivateKey jwtOzelAnahtar(KimlikAyarlari ayarlar) {
        return anahtarOku(ayarlar.jwt().ozelAnahtar(), RsaKeyConverters.pkcs8());
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAPublicKey acikAnahtar, RSAPrivateKey ozelAnahtar) {
        RSAKey jwk = new RSAKey.Builder(acikAnahtar).privateKey(ozelAnahtar).keyID(anahtarKimligi(acikAnahtar)).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    /** auth'un kendi korumalı uçları (/me, admin) için; imza + süre + "iss" kontrol edilir. */
    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey acikAnahtar, KimlikAyarlari ayarlar) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(acikAnahtar).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ayarlar.jwt().yayinci()));
        return decoder;
    }

    /**
     * "kid": anahtarın parmak izi (RFC 7638). Token başlığına yazılır; ileride anahtar değiştirilirken
     * doğrulayan taraf hangi anahtarı kullanacağını buradan anlar (planlama §5.1).
     */
    private static String anahtarKimligi(RSAPublicKey acikAnahtar) {
        try {
            return new RSAKey.Builder(acikAnahtar).build().computeThumbprint().toString();
        } catch (JOSEException e) {
            throw new IllegalStateException("JWT anahtar kimliği hesaplanamadı", e);
        }
    }

    private static <T> T anahtarOku(Resource konum, Converter<InputStream, T> donusturucu) {
        try (InputStream girdi = konum.getInputStream()) {
            return donusturucu.convert(girdi);
        } catch (IOException e) {
            throw new IllegalStateException("JWT anahtarı okunamadı: " + konum.getDescription()
                    + ". Anahtar çiftini scripts/jwt-anahtar-uret.sh ile üretin.", e);
        }
    }
}
