package com.sinema.booking.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // @CreatedDate / @LastModifiedDate alanlarını doldurur
@EnableConfigurationProperties(RezervasyonAyarlari.class)
public class RezervasyonYapilandirmasi {

    /**
     * Hold süresi ve satış kapanışı kararlarının tek zaman kaynağı (§2.4-10); testte sabitlenebilir.
     * Zaman SQL'e parametre olarak verilir (NOW() kullanılmaz), böylece kontrol ile yazma aynı "şimdi"yi görür.
     */
    @Bean
    public Clock saat() {
        return Clock.systemUTC();
    }
}
