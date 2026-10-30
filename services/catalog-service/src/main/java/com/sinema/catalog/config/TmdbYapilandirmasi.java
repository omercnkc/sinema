package com.sinema.catalog.config;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

/**
 * TMDB'ye giden HTTP istemcisi. Zaman aşımları kısa: admin formu beklemesin, TMDB yavaşsa
 * hızlıca 503 dönülsün ve elle giriş yapılabilsin (fail fast).
 */
@Configuration
@EnableScheduling // TMDB verisi tazelik job'ı (K45)
@EnableConfigurationProperties(TmdbAyarlari.class)
public class TmdbYapilandirmasi {

    @Bean
    public RestClient tmdbRestClient(TmdbAyarlari ayarlar) {
        HttpClient httpIstemcisi = HttpClient.newBuilder().connectTimeout(ayarlar.baglantiZamanAsimi()).build();
        JdkClientHttpRequestFactory istekFabrikasi = new JdkClientHttpRequestFactory(httpIstemcisi);
        istekFabrikasi.setReadTimeout(ayarlar.okumaZamanAsimi());
        RestClient.Builder kurucu = RestClient.builder()
                .baseUrl(ayarlar.tabanAdres())
                .requestFactory(istekFabrikasi);
        if (ayarlar.tokenVar()) {
            kurucu.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ayarlar.token());
        }
        return kurucu.build();
    }
}
