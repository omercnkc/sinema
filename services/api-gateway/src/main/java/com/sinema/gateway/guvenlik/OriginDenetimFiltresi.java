package com.sinema.gateway.guvenlik;

import com.sinema.gateway.config.GatewayAyarlari;
import com.sinema.gateway.hata.ProblemJsonYazici;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Cookie ile çalışan uçlar (refresh, logout) için CSRF savunması (planlama §6.6), SameSite=Strict'e ek katman:
 * tarayıcı, POST isteğinde isteği başlatan sayfanın adresini "Origin" başlığına yazar ve JavaScript bunu
 * değiştiremez. Origin izinli frontend değilse istek servise hiç gitmez.
 * CORS bunu tek başına sağlamaz: CORS yalnızca cevabı okumayı engeller, isteğin gönderilmesini değil.
 */
@Component
public class OriginDenetimFiltresi implements WebFilter, Ordered {

    public static final String IZINSIZ_ORIGIN_KODU = "ORIGIN_NOT_ALLOWED";
    private static final Set<String> CEREZLI_YOLLAR = Set.of("/api/v1/auth/refresh", "/api/v1/auth/logout");

    private final Set<String> izinliOriginler;

    public OriginDenetimFiltresi(GatewayAyarlari.Cors corsAyarlari) {
        this.izinliOriginler = Set.copyOf(corsAyarlari.izinliOriginler());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain zincir) {
        if (!denetlenecekMi(exchange)) {
            return zincir.filter(exchange);
        }
        String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
        if (origin == null || !izinliOriginler.contains(origin)) {
            return ProblemJsonYazici.yaz(exchange, HttpStatus.FORBIDDEN, IZINSIZ_ORIGIN_KODU,
                    "Bu istek izin verilen bir adresten gelmedi.");
        }
        return zincir.filter(exchange);
    }

    private static boolean denetlenecekMi(ServerWebExchange exchange) {
        return HttpMethod.POST.equals(exchange.getRequest().getMethod())
                && CEREZLI_YOLLAR.contains(exchange.getRequest().getPath().value());
    }

    // Korelasyon kimliğinden sonra, güvenlik zincirinden (-100) önce
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
