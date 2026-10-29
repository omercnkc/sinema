package com.sinema.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableJpaAuditing // @CreatedDate / @LastModifiedDate alanlarını doldurur
@EnableConfigurationProperties(KimlikAyarlari.class)
public class KimlikYapilandirmasi {

    @Bean
    public PasswordEncoder parolaKodlayici(KimlikAyarlari ayarlar) {
        return new BCryptPasswordEncoder(ayarlar.bcryptMaliyeti());
    }
}
