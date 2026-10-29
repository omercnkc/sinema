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
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Token İMZALAMA tarafı; yalnızca auth-service'te vardır, çünkü private key yalnızca burada bulunur.
 * Doğrulama (public key, JwtDecoder) tüm servislerde olduğu gibi sinema-common'dan gelir.
 */
@Configuration
public class JwtYapilandirmasi {

    @Bean
    public RSAPrivateKey jwtOzelAnahtar(KimlikAyarlari ayarlar) {
        Resource konum = ayarlar.jwt().ozelAnahtar();
        try (InputStream girdi = konum.getInputStream()) {
            return RsaKeyConverters.pkcs8().convert(girdi);
        } catch (IOException e) {
            throw new IllegalStateException("JWT private key okunamadı: " + konum.getDescription()
                    + ". Anahtar çiftini scripts/jwt-anahtar-uret.sh ile üretin.", e);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAPublicKey jwtAcikAnahtar, RSAPrivateKey jwtOzelAnahtar) {
        RSAKey jwk = new RSAKey.Builder(jwtAcikAnahtar).privateKey(jwtOzelAnahtar)
                .keyID(anahtarKimligi(jwtAcikAnahtar)).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
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
}
