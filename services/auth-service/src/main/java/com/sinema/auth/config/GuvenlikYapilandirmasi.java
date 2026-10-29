package com.sinema.auth.config;

import com.sinema.common.guvenlik.ProblemJsonErisimReddiIsleyicisi;
import com.sinema.common.guvenlik.ProblemJsonGirisNoktasi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * auth-service'in HTTP güvenliği: oturum (session) yok, her istek kendi token'ıyla gelir.
 * Kayıt, giriş ve sağlık kontrolü herkese açık; geri kalan her şey geçerli bir JWT ister (varsayılan kapalı).
 */
@Configuration
public class GuvenlikYapilandirmasi {

    @Bean
    public SecurityFilterChain guvenlikZinciri(HttpSecurity http, ProblemJsonGirisNoktasi girisNoktasi,
            ProblemJsonErisimReddiIsleyicisi erisimReddiIsleyicisi) {
        http
                // CSRF, tarayıcının otomatik gönderdiği oturum çerezine karşıdır; burada kimlik Authorization başlığında.
                // Refresh cookie'si (alt görev 4) SameSite=Strict + gateway'de Origin kontrolüyle korunur.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(oturum -> oturum.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(yetki -> yetki
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(kaynak -> kaynak
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(girisNoktasi))
                .exceptionHandling(hata -> hata
                        .authenticationEntryPoint(girisNoktasi)
                        .accessDeniedHandler(erisimReddiIsleyicisi));
        return http.build();
    }
}
