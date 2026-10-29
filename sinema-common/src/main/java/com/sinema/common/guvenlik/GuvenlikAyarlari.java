package com.sinema.common.guvenlik;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * Her servlet servisin "sinema.guvenlik" ayarları.
 *
 * @param jwt       token doğrulama
 * @param acikYollar token istemeyen yollar, "METOT /desen" ya da yalnızca "/desen" (her metot);
 *                  ör. "GET /api/v1/catalog/**". Listede olmayan her şey kapalıdır.
 */
@Validated
@ConfigurationProperties(prefix = "sinema.guvenlik")
public record GuvenlikAyarlari(@Valid @NotNull Jwt jwt, List<String> acikYollar) {

    public GuvenlikAyarlari {
        acikYollar = acikYollar == null ? List.of() : List.copyOf(acikYollar);
    }

    /**
     * @param yayinci     beklenen "iss" (auth-service'in imzaladığı ad)
     * @param acikAnahtar auth'un public key'i (X.509 PEM); private key servislere hiç verilmez
     */
    public record Jwt(@NotBlank String yayinci, @NotNull Resource acikAnahtar) {
    }
}
