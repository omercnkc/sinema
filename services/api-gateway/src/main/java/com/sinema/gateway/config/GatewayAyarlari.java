package com.sinema.gateway.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Gateway ayarları. JWT kısmı servislerle aynı ad altında ("sinema.guvenlik.jwt"): aynı token'ı aynı kurallarla
 * doğrularlar. CORS listesi koda gömülmez; "*" kullanılmaz (rules §5.6).
 */
public final class GatewayAyarlari {

    private GatewayAyarlari() {
    }

    @ConfigurationProperties(prefix = "sinema.guvenlik.jwt")
    public record Jwt(String yayinci, Resource acikAnahtar) {
    }

    /**
     * @param izinliOriginler tarayıcıdan API'ye istek atabilecek frontend adresleri (ör. http://localhost:4200)
     */
    @ConfigurationProperties(prefix = "sinema.gateway.cors")
    public record Cors(List<String> izinliOriginler) {

        public Cors {
            izinliOriginler = izinliOriginler == null ? List.of() : List.copyOf(izinliOriginler);
        }
    }

    /** İstek sınırı kuralları; sırayla denenir, ilk eşleşen uygulanır (özelden genele yazılmalı). */
    @ConfigurationProperties(prefix = "sinema.gateway.hiz-siniri")
    public record HizSiniri(List<Kural> kurallar) {

        public HizSiniri {
            kurallar = kurallar == null ? List.of() : List.copyOf(kurallar);
        }

        /**
         * "Aynı IP'den {sure} içinde en çok {istek} istek" kuralı.
         *
         * @param ad       kuralın adı (Redis anahtarında kullanılır, kurallar birbirinin sayacını etkilemez)
         * @param yol      "METOT /desen" veya "/desen"
         * @param aniArtis kısa sürede art arda izin verilen istek sayısı; boşsa {istek}
         * @param kati     Redis'e ulaşılamazsa isteği reddet (fail-closed); false ise geçir (fail-open, K38)
         */
        public record Kural(String ad, String yol, int istek, Duration sure, Integer aniArtis, boolean kati) {
        }
    }
}
