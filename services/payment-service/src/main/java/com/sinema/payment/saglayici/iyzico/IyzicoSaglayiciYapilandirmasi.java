package com.sinema.payment.saglayici.iyzico;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** iyzico adaptörü yalnızca saglayici=IYZICO iken kurulur (varsayılan, K49); anahtarlar eksikse servis açılmaz. */
@Configuration
@ConditionalOnProperty(name = "sinema.payment.saglayici", havingValue = "IYZICO")
@EnableConfigurationProperties(IyzicoAyarlari.class)
public class IyzicoSaglayiciYapilandirmasi {

    // iyzico örneğindeki biçim: epoch ms + rastgele haneler (her istekte farklı)
    private static final int RASTGELE_HANE_SINIRI = 1_000_000_000;

    @Bean
    public IyzicoOdemeSaglayicisi iyzicoOdemeSaglayicisi(IyzicoAyarlari ayarlar, Clock saat) {
        HttpClient httpIstemcisi = HttpClient.newBuilder().connectTimeout(ayarlar.baglantiZamanAsimi()).build();
        JdkClientHttpRequestFactory istekFabrikasi = new JdkClientHttpRequestFactory(httpIstemcisi);
        istekFabrikasi.setReadTimeout(ayarlar.okumaZamanAsimi());
        // Spring 7 sıkıştırma açıkken isteğe iki ayrı "Accept-Encoding" başlığı (gzip, deflate) ekler; iyzico'nun
        // güvenlik duvarı böyle bir isteği HTTP 200 + boş gövdeyle reddediyor (2026-10-31, sandbox'ta ölçüldü).
        istekFabrikasi.enableCompression(false);
        RestClient istemci = RestClient.builder()
                .baseUrl(ayarlar.tabanAdres().toString())
                .requestFactory(istekFabrikasi)
                .build();
        return new IyzicoOdemeSaglayicisi(istemci,
                new IyzicoImzalayici(ayarlar.apiAnahtari(), ayarlar.gizliAnahtar(), rastgeleAnahtar(saat)), saat);
    }

    private static Supplier<String> rastgeleAnahtar(Clock saat) {
        SecureRandom rastgele = new SecureRandom();
        return () -> saat.millis() + String.format("%09d", rastgele.nextInt(RASTGELE_HANE_SINIRI));
    }
}
