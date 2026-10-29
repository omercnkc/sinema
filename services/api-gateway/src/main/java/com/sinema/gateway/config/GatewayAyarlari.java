package com.sinema.gateway.config;

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
}
