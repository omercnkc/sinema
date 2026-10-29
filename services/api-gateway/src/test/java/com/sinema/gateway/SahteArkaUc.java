package com.sinema.gateway;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * Testlerde dört servisin yerine geçen küçük sunucu: gelen her isteği kaydeder, 200 döner.
 * Böylece bir isteğin gateway'den geçip servise ULAŞIP ulaşmadığı görülür.
 */
public final class SahteArkaUc {

    private final List<GelenIstek> gelenler = new CopyOnWriteArrayList<>();
    private final DisposableServer sunucu = HttpServer.create().port(0)
            .handle((istek, cevap) -> {
                String kimlik = istek.requestHeaders().get("X-Correlation-Id");
                gelenler.add(new GelenIstek(istek.uri(), istek.requestHeaders().get(HttpHeaders.AUTHORIZATION), kimlik));
                // Gerçek servisler gibi korelasyon başlığını geri döner (gateway bunu tekrarlamamalı)
                return cevap.status(200).header("X-Correlation-Id", kimlik == null ? "" : kimlik)
                        .sendString(Mono.just("arka-uc"));
            })
            .bindNow();

    /** Gateway'in dört servis adresini bu sunucuya yönlendirir. */
    public void adresleriKaydet(DynamicPropertyRegistry kayit) {
        String adres = "http://localhost:" + sunucu.port();
        for (String servis : List.of("AUTH", "CATALOG", "BOOKING", "PAYMENT")) {
            kayit.add(servis + "_SERVICE_URL", () -> adres);
        }
    }

    public List<GelenIstek> gelenler() {
        return gelenler;
    }

    public int gelenSayisi() {
        return gelenler.size();
    }

    public void temizle() {
        gelenler.clear();
    }

    public void kapat() {
        sunucu.disposeNow();
    }

    public record GelenIstek(String yol, String yetkiBasligi, String korelasyonKimligi) {
    }
}
