package com.sinema.catalog.config;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Katalogun iş kuralı değerleri (sinema.katalog.*); koda gömülmez (planlama §6.4).
 *
 * @param seansArasiPayi             reklam/temizlik payı; seans bitişi = başlangıç + film süresi + bu pay (K26)
 * @param varsayilanSatisKapanisOfseti satış kapanışı = başlangıç + ofset (K10: +10 dk); seans bazında değiştirilebilir
 * @param isSaatDilimi               "bugünün seansları" gibi gün hesapları bu dilimde yapılır; saklama UTC (§2.4-9)
 */
@ConfigurationProperties(prefix = "sinema.katalog")
public record KatalogAyarlari(Duration seansArasiPayi, Duration varsayilanSatisKapanisOfseti, ZoneId isSaatDilimi) {
}
