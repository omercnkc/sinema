package com.sinema.common.yapilandirma;

import com.sinema.common.servisarasi.BagimlilikHatalari;
import com.sinema.common.servisarasi.GuvenliYontemYenidenDeneyici;
import com.sinema.common.servisarasi.KorelasyonKimligiEkleyici;
import com.sinema.common.servisarasi.ServisIstemcisiAyarlari;
import com.sinema.common.servisarasi.ServisTokeniEkleyici;
import com.sinema.common.servisarasi.ServisTokeniSaglayici;
import com.sinema.common.servisarasi.SinemaHataCozucu;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Feign ile başka servisi çağıran servislerin (booking → catalog, payment → booking) ortak altyapısı
 * (rules §5.7): servis JWT'si + korelasyon kimliği iletimi, ortak hata çözümü, GET'e özel yeniden deneme,
 * zaman aşımları ve devre kesici.
 *
 * Bean'ler uygulama context'inde tanımlıdır; Spring Cloud OpenFeign her istemcinin alt context'inde bunları
 * üst context'ten bulur, yani tüm Feign istemcilerine uygulanır. Bu yüzden yalnızca iç servis çağrıları için
 * Feign kullanılır (dış API'ler, ör. TMDB, RestClient ile).
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
@ConditionalOnProperty(prefix = "sinema.servis-istemcisi", name = "auth-adresi")
@EnableConfigurationProperties(ServisIstemcisiAyarlari.class)
public class SinemaServisIstemcisiOtomatikYapilandirma {

    @Bean
    @ConditionalOnMissingBean
    public ServisTokeniSaglayici servisTokeniSaglayici(ServisIstemcisiAyarlari ayarlar,
            @Value("${spring.application.name}") String istemciKimligi) {
        HttpClient httpIstemcisi = HttpClient.newBuilder().connectTimeout(ayarlar.baglantiZamanAsimi()).build();
        JdkClientHttpRequestFactory istekFabrikasi = new JdkClientHttpRequestFactory(httpIstemcisi);
        istekFabrikasi.setReadTimeout(ayarlar.okumaZamanAsimi());
        RestClient authIstemcisi = RestClient.builder()
                .baseUrl(ayarlar.authAdresi().toString())
                .requestFactory(istekFabrikasi)
                .build();
        return new ServisTokeniSaglayici(authIstemcisi, istemciKimligi, ayarlar.sir(),
                ayarlar.tokenYenilemePayi(), Clock.systemUTC());
    }

    @Bean
    public ServisTokeniEkleyici servisTokeniEkleyici(ServisTokeniSaglayici servisTokeniSaglayici) {
        return new ServisTokeniEkleyici(servisTokeniSaglayici);
    }

    @Bean
    public KorelasyonKimligiEkleyici korelasyonKimligiEkleyici() {
        return new KorelasyonKimligiEkleyici();
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorDecoder sinemaHataCozucu() {
        return new SinemaHataCozucu();
    }

    @Bean
    @ConditionalOnMissingBean
    public Retryer guvenliYontemYenidenDeneyici(ServisIstemcisiAyarlari ayarlar) {
        ServisIstemcisiAyarlari.YenidenDeneme deneme = ayarlar.yenidenDeneme();
        return new GuvenliYontemYenidenDeneyici(new Retryer.Default(
                deneme.ilkBekleme().toMillis(), deneme.enUzunBekleme().toMillis(), deneme.enFazlaDeneme()));
    }

    /** Feign'in zaman aşımları; yönlendirme izlenmez (iç servisler yönlendirme yapmaz, yapıyorsa hatadır). */
    @Bean
    @ConditionalOnMissingBean
    public Request.Options feignZamanAsimlari(ServisIstemcisiAyarlari ayarlar) {
        return new Request.Options(ayarlar.baglantiZamanAsimi(), ayarlar.okumaZamanAsimi(), false);
    }

    /**
     * Devre kesici: yalnızca Resilience4j olan serviste. Zaman sınırı Feign'in okuma zaman aşımıdır; Resilience4j'nin
     * kendi TimeLimiter'ı ve thread havuzu servis ayarında kapatılır (aynı thread'de kalınca MDC'deki korelasyon
     * kimliği de interceptor'a ulaşır).
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Resilience4JCircuitBreakerFactory.class)
    static class DevreKesiciYapilandirmasi {

        @Bean
        public Customizer<Resilience4JCircuitBreakerFactory> sinemaDevreKesiciAyari(ServisIstemcisiAyarlari ayarlar) {
            ServisIstemcisiAyarlari.DevreKesici devreKesici = ayarlar.devreKesici();
            CircuitBreakerConfig yapilandirma = CircuitBreakerConfig.custom()
                    .slidingWindowSize(devreKesici.pencereBoyutu())
                    .minimumNumberOfCalls(devreKesici.enAzCagri())
                    .failureRateThreshold(devreKesici.hataOraniEsigi())
                    .waitDurationInOpenState(devreKesici.acikKalmaSuresi())
                    .ignoreException(BagimlilikHatalari::isSonucuMu)
                    .build();
            return fabrika -> fabrika.configureDefault(kimlik -> new Resilience4JConfigBuilder(kimlik)
                    .circuitBreakerConfig(yapilandirma)
                    .build());
        }
    }
}
