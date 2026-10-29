package com.sinema.gateway.korelasyon;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Korelasyon kimliğinin ÜRETİLDİĞİ yer (planlama §6.6): her istek gateway'den girer, kimlik burada atanır,
 * servise giden isteğin başlığına yazılır, servis de aynı kimliği kendi loglarında kullanır.
 * Güvenlik filtresinden önce çalışır: reddedilen isteklerin hata cevabında da kimlik bulunur.
 */
@Component
public class KorelasyonKimligiFiltresi implements WebFilter, Ordered {

    public static final String HTTP_BASLIGI = "X-Correlation-Id";
    public static final String KIMLIK_OZNITELIGI = KorelasyonKimligiFiltresi.class.getName() + ".kimlik";

    // Servislerdeki filtreyle aynı kural: dışarıdan gelen değer loglara yazılacağı için sınırlanır
    private static final Pattern GECERLI_KIMLIK = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain zincir) {
        String kimlik = gecerliKimlikVeyaYeni(exchange.getRequest().getHeaders().getFirst(HTTP_BASLIGI));
        exchange.getAttributes().put(KIMLIK_OZNITELIGI, kimlik);
        // Servis de aynı başlığı döner ve gateway onu cevaba ekler; gönderimden hemen önce 'set' edince tek değer kalır
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(HTTP_BASLIGI, kimlik);
            return Mono.empty();
        });
        ServerHttpRequest servisIstegi = exchange.getRequest().mutate()
                .headers(basliklar -> basliklar.set(HTTP_BASLIGI, kimlik))
                .build();
        return zincir.filter(exchange.mutate().request(servisIstegi).build());
    }

    private static String gecerliKimlikVeyaYeni(String gelen) {
        return gelen != null && GECERLI_KIMLIK.matcher(gelen).matches() ? gelen : UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
