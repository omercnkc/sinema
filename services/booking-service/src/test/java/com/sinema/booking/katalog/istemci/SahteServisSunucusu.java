package com.sinema.booking.katalog.istemci;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * Feign istemcisini gerçek HTTP üzerinden test etmek için auth ve catalog'un yerine geçen küçük sunucu.
 * JDK'nın yerleşik HttpServer'ı: yeni test bağımlılığı (WireMock vb.) gerekmez.
 * Her yol için cevap testte ayarlanır; gelen istekler (başlık + gövde) kaydedilir.
 */
final class SahteServisSunucusu {

    private final HttpServer sunucu;
    private final Map<String, Cevap> cevaplar = new ConcurrentHashMap<>();
    private final List<GelenIstek> gelenIstekler = new CopyOnWriteArrayList<>();

    SahteServisSunucusu() throws IOException {
        sunucu = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        sunucu.createContext("/", this::isle);
        // Zaman aşımı testinde bekleyen istek diğerlerini tıkamasın
        sunucu.setExecutor(Executors.newCachedThreadPool());
        sunucu.start();
    }

    String adres() {
        return "http://localhost:" + sunucu.getAddress().getPort();
    }

    void cevapla(String yol, int durum, String jsonGovde) {
        cevaplar.put(yol, new Cevap(durum, jsonGovde, 0));
    }

    void gecikmeliCevapla(String yol, long gecikmeMs) {
        cevaplar.put(yol, new Cevap(200, "{}", gecikmeMs));
    }

    List<GelenIstek> istekler(String yol) {
        return gelenIstekler.stream().filter(istek -> istek.yol().equals(yol)).toList();
    }

    void sifirla() {
        cevaplar.clear();
        gelenIstekler.clear();
    }

    void durdur() {
        sunucu.stop(0);
    }

    private void isle(HttpExchange takas) throws IOException {
        String yol = takas.getRequestURI().getPath();
        String govde = new String(takas.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        gelenIstekler.add(new GelenIstek(yol, takas.getRequestMethod(),
                takas.getRequestHeaders().getFirst("Authorization"),
                takas.getRequestHeaders().getFirst("X-Correlation-Id"), govde));
        Cevap cevap = cevaplar.getOrDefault(yol, new Cevap(404, "{}", 0));
        bekle(cevap.gecikmeMs());
        byte[] baytlar = cevap.govde().getBytes(StandardCharsets.UTF_8);
        takas.getResponseHeaders().add("Content-Type", "application/json");
        takas.sendResponseHeaders(cevap.durum(), baytlar.length);
        try (OutputStream cikti = takas.getResponseBody()) {
            cikti.write(baytlar);
        } catch (IOException e) {
            // İstemci zaman aşımıyla bağlantıyı kapatmışsa yazma başarısız olur; testte beklenen durum
        }
    }

    private static void bekle(long ms) {
        if (ms == 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    record GelenIstek(String yol, String metot, String yetki, String korelasyonKimligi, String govde) {
    }

    private record Cevap(int durum, String govde, long gecikmeMs) {
    }
}
