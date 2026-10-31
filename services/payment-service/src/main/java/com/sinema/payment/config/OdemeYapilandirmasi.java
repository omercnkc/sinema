package com.sinema.payment.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // @CreatedDate / @LastModifiedDate alanlarını doldurur
@EnableConfigurationProperties(OdemeAyarlari.class)
public class OdemeYapilandirmasi {

    /** Token ömrü ve mutabakat kararlarının tek zaman kaynağı; testte sabitlenebilir. */
    @Bean
    public Clock saat() {
        return Clock.systemUTC();
    }
}
