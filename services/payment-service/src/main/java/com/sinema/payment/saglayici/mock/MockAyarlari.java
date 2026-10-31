package com.sinema.payment.saglayici.mock;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Mock sağlayıcı ayarları (sinema.payment.mock.*), yalnızca saglayici=MOCK iken okunur.
 *
 * @param sayfaAdresi sahte ödeme sayfasının taban adresi (gateway üzerinden); token sonuna eklenir
 * @param tokenOmru   iyzico ile aynı (30 dk) ki mutabakat davranışı yerelde de aynı görünsün
 */
@Validated
@ConfigurationProperties(prefix = "sinema.payment.mock")
public record MockAyarlari(
        @NotNull URI sayfaAdresi,
        @NotNull @DefaultValue("30m") Duration tokenOmru) {
}
