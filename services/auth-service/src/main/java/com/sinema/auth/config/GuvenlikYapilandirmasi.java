package com.sinema.auth.config;

import com.sinema.auth.oturum.service.ErisimTokeniUretici;
import com.sinema.common.guvenlik.ProblemJsonErisimReddiIsleyicisi;
import com.sinema.common.guvenlik.ProblemJsonGirisNoktasi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * auth-service'in HTTP güvenliği: oturum (session) yok, her istek kendi token'ıyla gelir.
 * Herkese açık olanlar açıkça listelenir; geri kalan her şey geçerli bir JWT ister (varsayılan kapalı).
 */
@Configuration
@EnableMethodSecurity // @PreAuthorize
public class GuvenlikYapilandirmasi {

    @Bean
    public SecurityFilterChain guvenlikZinciri(HttpSecurity http, ProblemJsonGirisNoktasi girisNoktasi,
            ProblemJsonErisimReddiIsleyicisi erisimReddiIsleyicisi) {
        http
                // CSRF, tarayıcının otomatik gönderdiği oturum çerezine karşıdır. Access token Authorization başlığında;
                // refresh cookie'si ise SameSite=Strict + dar Path ile ve gateway'deki Origin kontrolüyle korunur.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(oturum -> oturum.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(yetki -> yetki
                        // refresh ve logout JWT değil cookie ile çalışır (access token süresi dolmuş olabilir)
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        // URL kuralı + metottaki @PreAuthorize: biri unutulsa diğeri korur
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(kaynak -> kaynak
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolDonusturucu()))
                        .authenticationEntryPoint(girisNoktasi))
                .exceptionHandling(hata -> hata
                        .authenticationEntryPoint(girisNoktasi)
                        .accessDeniedHandler(erisimReddiIsleyicisi));
        return http.build();
    }

    /** Token'daki "role" (ör. ADMIN) → Spring yetkisi "ROLE_ADMIN"; hasRole('ADMIN') bunu arar. */
    private static JwtAuthenticationConverter rolDonusturucu() {
        JwtGrantedAuthoritiesConverter yetkiler = new JwtGrantedAuthoritiesConverter();
        yetkiler.setAuthoritiesClaimName(ErisimTokeniUretici.ROL_ALANI);
        yetkiler.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter donusturucu = new JwtAuthenticationConverter();
        donusturucu.setJwtGrantedAuthoritiesConverter(yetkiler);
        return donusturucu;
    }
}
