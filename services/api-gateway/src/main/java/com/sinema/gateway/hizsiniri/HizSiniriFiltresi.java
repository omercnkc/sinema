package com.sinema.gateway.hizsiniri;

import com.sinema.gateway.config.GatewayAyarlari;
import com.sinema.gateway.hata.ProblemJsonYazici;
import java.net.InetSocketAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * IP başına istek sınırı (planlama §6.6, K37, K38). Sayma işi gateway'in RedisRateLimiter'ındadır
 * (Redis'te atomik Lua betiği, çok gateway örneğinde de doğru); bu filtre yalnızca hangi kuralın
 * uygulanacağına ve cevaba karar verir.
 *
 * Redis'e ulaşılamazsa RedisRateLimiter hatayı yutar, isteğe izin verir ve kalan hakkı -1 yazar.
 * Bu işaret görülünce: katı kuralda (giriş/kayıt) istek reddedilir, diğerlerinde geçer.
 * Güvenlik filtresinden önce çalışır: sınırı aşan istek JWT doğrulamasına bile maliyet çıkarmaz.
 */
@Component
@EnableConfigurationProperties(GatewayAyarlari.HizSiniri.class)
public class HizSiniriFiltresi implements WebFilter, Ordered {

    public static final String SINIR_ASILDI_KODU = "RATE_LIMITED";
    public static final String SINIR_KULLANILAMIYOR_KODU = "RATE_LIMIT_UNAVAILABLE";
    private static final String REDIS_HATASI_ISARETI = "-1";
    private static final String BILINMEYEN_ADRES = "bilinmiyor";

    private static final Logger log = LoggerFactory.getLogger(HizSiniriFiltresi.class);

    private final RedisRateLimiter sinirlayici;
    private final List<HizKurali> kurallar;

    public HizSiniriFiltresi(RedisRateLimiter sinirlayici, GatewayAyarlari.HizSiniri ayarlar) {
        this.sinirlayici = sinirlayici;
        this.kurallar = ayarlar.kurallar().stream().map(HizKurali::new).toList();
        kurallar.forEach(kural -> sinirlayici.getConfig().put(kural.ad(), kural.redisAyari()));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain zincir) {
        HizKurali kural = eslesenKural(exchange.getRequest());
        if (kural == null) {
            return zincir.filter(exchange);
        }
        // RedisRateLimiter anahtarı "kuralAdı.ip" olarak kurar: giriş denemeleri genel sayaçtan ayrı tutulur
        return sinirlayici.isAllowed(kural.ad(), istemciAdresi(exchange.getRequest()))
                .flatMap(sonuc -> karar(sonuc, kural, exchange, zincir));
    }

    private Mono<Void> karar(RateLimiter.Response sonuc, HizKurali kural, ServerWebExchange exchange,
            WebFilterChain zincir) {
        if (REDIS_HATASI_ISARETI.equals(sonuc.getHeaders().get(sinirlayici.getRemainingHeader()))) {
            return redisErisilemedi(kural, exchange, zincir);
        }
        if (!sonuc.isAllowed()) {
            exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(kural.beklemeSaniyesi()));
            return ProblemJsonYazici.yaz(exchange, HttpStatus.TOO_MANY_REQUESTS, SINIR_ASILDI_KODU,
                    "Çok fazla istek gönderildi. Lütfen biraz bekleyip tekrar deneyin.");
        }
        return zincir.filter(exchange);
    }

    private Mono<Void> redisErisilemedi(HizKurali kural, ServerWebExchange exchange, WebFilterChain zincir) {
        if (!kural.kati()) {
            return zincir.filter(exchange); // fail-open: kullanılabilirlik öncelikli
        }
        // fail-closed: sınır sayılamıyorken giriş/kayıt kaba kuvvete açık kalmasın
        log.warn("Redis'e ulaşılamıyor; katı hız sınırı kuralı '{}' isteği reddetti", kural.ad());
        return ProblemJsonYazici.yaz(exchange, HttpStatus.SERVICE_UNAVAILABLE, SINIR_KULLANILAMIYOR_KODU,
                "Bu işlem şu anda yapılamıyor. Lütfen biraz sonra tekrar deneyin.");
    }

    private HizKurali eslesenKural(ServerHttpRequest istek) {
        return kurallar.stream()
                .filter(kural -> kural.eslesir(istek.getMethod(), istek.getPath().pathWithinApplication()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Doğrudan bağlanan istemcinin adresi. X-Forwarded-For'a bilerek bakılmaz: istemci bu başlığı
     * istediği gibi yazıp sınırı atlatabilirdi. Gateway'in önüne bir proxy konursa güvenilen proxy
     * sayısıyla (XForwardedRemoteAddressResolver) okunmalıdır.
     */
    private static String istemciAdresi(ServerHttpRequest istek) {
        InetSocketAddress adres = istek.getRemoteAddress();
        return adres != null && adres.getAddress() != null ? adres.getAddress().getHostAddress() : BILINMEYEN_ADRES;
    }

    // Korelasyon (en önce) ve Origin denetiminden sonra, güvenlik zincirinden (-100) önce
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
