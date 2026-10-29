package com.sinema.gateway.hata;

import com.sinema.gateway.korelasyon.KorelasyonKimligiFiltresi;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gateway'in kendi ürettiği hataları (401, 403, origin reddi) servislerle AYNI problem+json biçiminde yazar;
 * istemci hatanın gateway'den mi servisten mi geldiğini ayırt etmek zorunda kalmaz.
 * sinema-common servlet tabanlı olduğu için burada kullanılamaz; biçim bilinçli olarak orayla aynı tutulur.
 */
public final class ProblemJsonYazici {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ProblemJsonYazici() {
    }

    public static Mono<Void> yaz(ServerWebExchange exchange, HttpStatus durum, String kod, String detay) {
        Map<String, Object> govde = new LinkedHashMap<>();
        govde.put("instance", exchange.getRequest().getPath().value());
        govde.put("title", durum.getReasonPhrase());
        govde.put("status", durum.value());
        govde.put("detail", detay);
        govde.put("code", kod);
        govde.put("correlationId", exchange.getAttribute(KorelasyonKimligiFiltresi.KIMLIK_OZNITELIGI));
        govde.put("timestamp", Instant.now().toString());

        ServerHttpResponse cevap = exchange.getResponse();
        cevap.setStatusCode(durum);
        cevap.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return cevap.writeWith(Mono.fromSupplier(() -> cevap.bufferFactory().wrap(JSON.writeValueAsBytes(govde))));
    }
}
