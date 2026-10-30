package com.sinema.catalog.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // @CreatedDate / @LastModifiedDate alanlarını doldurur
@EnableConfigurationProperties(KatalogAyarlari.class)
public class KatalogYapilandirmasi {

    /** Zamana bağlı kurallar (yayım anı, seans kilidi) bu saati kullanır; testte sabitlenebilir. */
    @Bean
    public Clock saat() {
        return Clock.systemUTC();
    }
}
