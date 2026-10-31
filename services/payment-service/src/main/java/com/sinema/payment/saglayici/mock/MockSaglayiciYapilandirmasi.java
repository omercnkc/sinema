package com.sinema.payment.saglayici.mock;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Mock sağlayıcı yalnızca açıkça seçilince kurulur (K49); iyzico seçiliyken bu sınıfın hiçbir bean'i yoktur. */
@Configuration
@ConditionalOnProperty(name = "sinema.payment.saglayici", havingValue = "MOCK")
@EnableConfigurationProperties(MockAyarlari.class)
public class MockSaglayiciYapilandirmasi {

    @Bean
    public MockOdemeSaglayicisi mockOdemeSaglayicisi(MockAyarlari ayarlar, Clock saat) {
        return new MockOdemeSaglayicisi(ayarlar, saat);
    }
}
