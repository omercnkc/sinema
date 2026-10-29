package com.sinema.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * auth-service'in application.yml'deki "sinema.auth" ayarları; değerler koda gömülmez.
 * Geçersiz değerle servis açılmaz (@Validated).
 *
 * @param bcryptMaliyeti BCrypt iş faktörü; her +1 hesaplama süresini ikiye katlar
 */
@Validated
@ConfigurationProperties(prefix = "sinema.auth")
public record KimlikAyarlari(
        @Min(10) @Max(16) int bcryptMaliyeti,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Kilit kilit) {

    /**
     * @param yayinci          token'daki "iss"; doğrulayan servisler bunu da kontrol eder
     * @param erisimTokenSuresi access token ömrü (K36: 15 dk)
     * @param ozelAnahtar      PKCS#8 private key (yalnızca auth-service'te)
     * @param acikAnahtar      X.509 public key
     */
    public record Jwt(
            @NotBlank String yayinci,
            @NotNull Duration erisimTokenSuresi,
            @NotNull Resource ozelAnahtar,
            @NotNull Resource acikAnahtar) {
    }

    /**
     * @param esik kaç hatalı denemede kilitlenir (K36: 5)
     * @param sure kilit ne kadar sürer (K36: 5 dk)
     */
    public record Kilit(@Min(1) int esik, @NotNull Duration sure) {
    }
}
