package com.sinema.gateway.hizsiniri;

import com.sinema.gateway.config.GatewayAyarlari;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Ayar satırının çözülmüş, kullanıma hazır hali. "N istek / süre" ifadesini Redis token bucket ayarına çevirir:
 *  - her istek  T = süre(sn) jeton harcar
 *  - saniyede   N jeton dolar               → süre başına N istek
 *  - kova       aniArtis × T jeton tutar    → art arda en çok aniArtis istek
 * Örn. 10 istek / 1 dk: istek başına 60 jeton, saniyede 10 jeton → 6 sn'de bir istek, art arda 10.
 * (Redis betiği saniyede tam sayı jeton ister; dakikalık sınırı bu yüzden "pahalı istek" olarak ifade ederiz.)
 */
final class HizKurali {

    private final String ad;
    private final HttpMethod metot;
    private final PathPattern desen;
    private final boolean kati;
    private final RedisRateLimiter.Config redisAyari;
    private final long beklemeSaniyesi;

    HizKurali(GatewayAyarlari.HizSiniri.Kural ayar) {
        long sureSaniye = ayar.sure().toSeconds();
        if (sureSaniye < 1 || ayar.istek() < 1) {
            throw new IllegalArgumentException("Hız sınırı '" + ayar.ad() + "': istek ve süre (tam saniye) en az 1 olmalı");
        }
        String[] parcalar = ayar.yol().trim().split("\\s+");
        this.ad = ayar.ad();
        this.metot = parcalar.length == 2 ? HttpMethod.valueOf(parcalar[0].toUpperCase()) : null;
        this.desen = PathPatternParser.defaultInstance.parse(parcalar[parcalar.length - 1]);
        this.kati = ayar.kati();
        int jeton = Math.toIntExact(sureSaniye);
        int aniArtis = ayar.aniArtis() != null ? ayar.aniArtis() : ayar.istek();
        this.redisAyari = new RedisRateLimiter.Config()
                .setReplenishRate(ayar.istek())
                .setBurstCapacity((long) aniArtis * jeton)
                .setRequestedTokens(jeton);
        // Bir isteklik jetonun dolma süresi, yukarı yuvarlanmış
        this.beklemeSaniyesi = Math.max(1, (jeton + ayar.istek() - 1) / ayar.istek());
    }

    boolean eslesir(HttpMethod istekMetodu, PathContainer yol) {
        return (metot == null || metot.equals(istekMetodu)) && desen.matches(yol);
    }

    String ad() {
        return ad;
    }

    boolean kati() {
        return kati;
    }

    RedisRateLimiter.Config redisAyari() {
        return redisAyari;
    }

    long beklemeSaniyesi() {
        return beklemeSaniyesi;
    }
}
