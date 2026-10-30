package com.sinema.catalog.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * TMDB entegrasyonu (sinema.tmdb.*). Token boşsa TMDB özellikleri 503 verir, elle film girişi çalışmaya
 * devam eder (K18).
 *
 * @param token              v4 "API Read Access Token" (.env: TMDB_API_TOKEN); loglanmaz
 * @param tazelikSuresi      TMDB verisi bu süreden eskiyse yenilenir (K45: 3 ay; şartlardaki üst sınır 6 ay)
 * @param yenilemeZamanlamasi yenileme job'ının cron ifadesi
 * @param yenilemePaketi     bir çalışmada yenilenen en çok film (TMDB hız sınırına saygı)
 */
@ConfigurationProperties(prefix = "sinema.tmdb")
public record TmdbAyarlari(
        String tabanAdres,
        String token,
        String dil,
        String bolge,
        Duration baglantiZamanAsimi,
        Duration okumaZamanAsimi,
        int oyuncuSiniri,
        Duration tazelikSuresi,
        String yenilemeZamanlamasi,
        int yenilemePaketi) {

    public boolean tokenVar() {
        return token != null && !token.isBlank();
    }
}
