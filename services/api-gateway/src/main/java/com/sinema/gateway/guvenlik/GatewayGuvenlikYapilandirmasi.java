package com.sinema.gateway.guvenlik;

import com.sinema.gateway.config.GatewayAyarlari;
import com.sinema.gateway.hata.ProblemJsonYazici;
import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway'deki ilk savunma hattı (planlama §5.4): token ön doğrulaması + rota bazlı rol kontrolü + CORS.
 * Servisler aynı token'ı ayrıca kendileri doğrular; gateway atlanırsa (iç ağdan doğrudan çağrı) da korunurlar.
 * Authorization başlığı servise değiştirilmeden iletilir.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties({GatewayAyarlari.Jwt.class, GatewayAyarlari.Cors.class})
public class GatewayGuvenlikYapilandirmasi {

    private static final String KULLANICI = "USER";
    private static final String ADMIN = "ADMIN";
    private static final String ODEME_DONUS_YOLU = "/api/v1/payments/callback/iyzico";

    @Bean
    public SecurityWebFilterChain gatewayGuvenlikZinciri(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // cookie'li uçlar OriginDenetimFiltresi ile korunur
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> { }) // CorsConfigurationSource bean'ini kullanır
                .authorizeExchange(yetki -> yetki
                        // İç uçlar dışarıya hiç açılmaz; zaten rota da yok (planlama §5.5)
                        .pathMatchers("/internal/**").denyAll()
                        .pathMatchers("/actuator/health/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        // Misafir görüntüleme (K3): katalog, koltuk doluluğu; QR bilet doğrulama (K30)
                        .pathMatchers(HttpMethod.GET, "/api/v1/catalog/**",
                                "/api/v1/bookings/showtimes/*/seat-status", "/api/v1/tickets/*").permitAll()
                        .pathMatchers(HttpMethod.POST, ODEME_DONUS_YOLU).permitAll()
                        .pathMatchers("/api/v1/admin/**").hasRole(ADMIN)
                        // SERVICE token'ı gateway'den geçemez: servisler birbirine iç ağdan gider
                        .pathMatchers("/api/v1/**").hasAnyRole(KULLANICI, ADMIN)
                        // Geri kalan (actuator metrikleri vb.) dışarıya kapalı
                        .anyExchange().denyAll())
                .oauth2ResourceServer(kaynak -> kaynak
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder).jwtAuthenticationConverter(rolDonusturucu()))
                        .authenticationEntryPoint((exchange, hata) -> yetkisiz(exchange)))
                .exceptionHandling(hata -> hata
                        .authenticationEntryPoint((exchange, istisna) -> yetkisiz(exchange))
                        .accessDeniedHandler((exchange, istisna) -> ProblemJsonYazici.yaz(exchange,
                                HttpStatus.FORBIDDEN, "FORBIDDEN", "Bu işlem için yetkiniz yok.")))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(GatewayAyarlari.Jwt ayarlar) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withPublicKey(acikAnahtarOku(ayarlar)).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ayarlar.yayinci()));
        return decoder;
    }

    /**
     * Ayrı origin (K37): frontend (ör. :4200) gateway'e (:8080) başka origin'den gelir.
     * credentials=true: refresh cookie'si gidebilsin. Bu yüzden "*" kullanılamaz, adresler açıkça listelenir.
     * iyzico dönüşü CORS dışında: o istek iyzico sayfasından gelen tam sayfa form POST'udur (XHR değil);
     * CORS uygulansaydı iyzico'nun Origin'i izinli olmadığı için reddedilirdi.
     */
    @Bean
    public CorsConfigurationSource corsKaynagi(GatewayAyarlari.Cors ayarlar) {
        CorsConfiguration yapilandirma = new CorsConfiguration();
        yapilandirma.setAllowedOrigins(ayarlar.izinliOriginler());
        yapilandirma.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        yapilandirma.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-Correlation-Id"));
        // Tarayıcı JS'i yalnızca açıkça izin verilen cevap başlıklarını okuyabilir (Angular 429'da Retry-After'ı gösterir)
        yapilandirma.setExposedHeaders(List.of("X-Correlation-Id", HttpHeaders.RETRY_AFTER));
        yapilandirma.setAllowCredentials(true);
        yapilandirma.setMaxAge(Duration.ofHours(1));
        return exchange -> ODEME_DONUS_YOLU.equals(exchange.getRequest().getPath().value()) ? null : yapilandirma;
    }

    private static Mono<Void> yetkisiz(ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return ProblemJsonYazici.yaz(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Bu işlem için geçerli bir oturum gerekli.");
    }

    private static ReactiveJwtAuthenticationConverterAdapter rolDonusturucu() {
        JwtGrantedAuthoritiesConverter yetkiler = new JwtGrantedAuthoritiesConverter();
        yetkiler.setAuthoritiesClaimName("role");
        yetkiler.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter donusturucu = new JwtAuthenticationConverter();
        donusturucu.setJwtGrantedAuthoritiesConverter(yetkiler);
        return new ReactiveJwtAuthenticationConverterAdapter(donusturucu);
    }

    private static RSAPublicKey acikAnahtarOku(GatewayAyarlari.Jwt ayarlar) {
        try (InputStream girdi = ayarlar.acikAnahtar().getInputStream()) {
            return RsaKeyConverters.x509().convert(girdi);
        } catch (IOException e) {
            throw new IllegalStateException("JWT public key okunamadı: " + ayarlar.acikAnahtar().getDescription()
                    + ". Anahtar çiftini scripts/jwt-anahtar-uret.sh ile üretin.", e);
        }
    }
}
