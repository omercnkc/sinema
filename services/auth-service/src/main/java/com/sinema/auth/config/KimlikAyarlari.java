package com.sinema.auth.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * auth-service'in application.yml'deki "sinema.auth" ayarları. Sonraki alt görevlerde
 * JWT süresi, hesap kilidi eşikleri gibi değerler de buraya eklenir.
 *
 * @param bcryptMaliyeti BCrypt iş faktörü; her +1 hesaplama süresini ikiye katlar
 */
@Validated
@ConfigurationProperties(prefix = "sinema.auth")
public record KimlikAyarlari(@Min(10) @Max(16) int bcryptMaliyeti) {
}
