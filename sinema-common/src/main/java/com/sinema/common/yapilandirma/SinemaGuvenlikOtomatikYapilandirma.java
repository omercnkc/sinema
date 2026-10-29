package com.sinema.common.yapilandirma;

import com.sinema.common.guvenlik.GuvenlikAyarlari;
import com.sinema.common.guvenlik.GuvenlikKurallari;
import com.sinema.common.guvenlik.ProblemJsonErisimReddiIsleyicisi;
import com.sinema.common.guvenlik.ProblemJsonGirisNoktasi;
import com.sinema.common.guvenlik.Roller;
import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Tüm servlet servislerin ortak güvenliği (planlama §5.4): JWT doğrulama + rol dönüşümü + "varsayılan kapalı" zincir.
 * Servis yalnızca kendi açık yollarını ayar olarak verir; kurallar burada tek yerdedir.
 *
 * Boot'un kendi varsayılan zincirinden ÖNCE işlenmeli: Boot, ortamda SecurityFilterChain yoksa kendi
 * zincirini kurar; bizimki önce tanımlanınca Boot'unki devre dışı kalır, iki zincir çakışmaz.
 */
@AutoConfiguration(beforeName = {
        "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration",
        "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
        "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration",
        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration",
        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration"})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SecurityFilterChain.class, JwtDecoder.class})
@EnableConfigurationProperties(GuvenlikAyarlari.class)
@EnableMethodSecurity // servisler @PreAuthorize kullanabilsin
public class SinemaGuvenlikOtomatikYapilandirma {

    /** Public key bean'i: doğrulayıcı kullanır, auth da imzalayıcıyı kurarken aynısını alır. */
    @Bean
    @ConditionalOnMissingBean
    public RSAPublicKey jwtAcikAnahtar(GuvenlikAyarlari ayarlar) {
        try (InputStream girdi = ayarlar.jwt().acikAnahtar().getInputStream()) {
            return RsaKeyConverters.x509().convert(girdi);
        } catch (IOException e) {
            throw new IllegalStateException("JWT public key okunamadı: " + ayarlar.jwt().acikAnahtar().getDescription()
                    + ". Anahtar çiftini scripts/jwt-anahtar-uret.sh ile üretin.", e);
        }
    }

    /** İmza (yalnızca auth'un private key'iyle atılmış olmalı) + süre (exp/nbf) + yayıncı (iss) kontrolü. */
    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(RSAPublicKey jwtAcikAnahtar, GuvenlikAyarlari ayarlar) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(jwtAcikAnahtar).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ayarlar.jwt().yayinci()));
        return decoder;
    }

    /** Token'daki "role" (ör. ADMIN) → Spring yetkisi "ROLE_ADMIN"; hasRole('ADMIN') bunu arar. */
    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter yetkiler = new JwtGrantedAuthoritiesConverter();
        yetkiler.setAuthoritiesClaimName(Roller.ROL_ALANI);
        yetkiler.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter donusturucu = new JwtAuthenticationConverter();
        donusturucu.setJwtGrantedAuthoritiesConverter(yetkiler);
        return donusturucu;
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain sinemaGuvenlikZinciri(HttpSecurity http, GuvenlikAyarlari ayarlar,
            JwtAuthenticationConverter jwtDonusturucu, ProblemJsonGirisNoktasi girisNoktasi,
            ProblemJsonErisimReddiIsleyicisi erisimReddiIsleyicisi,
            @Value("${spring.application.name}") String servisAdi) {
        http
                // CSRF, tarayıcının otomatik gönderdiği oturum çerezine karşıdır; kimlik Authorization başlığında.
                // (auth'un refresh cookie'si SameSite=Strict + dar Path + gateway'de Origin kontrolüyle korunur.)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(oturum -> oturum.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(yetki -> GuvenlikKurallari.uygula(yetki, ayarlar.acikYollar(), servisAdi))
                .oauth2ResourceServer(kaynak -> kaynak
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtDonusturucu))
                        .authenticationEntryPoint(girisNoktasi))
                .exceptionHandling(hata -> hata
                        .authenticationEntryPoint(girisNoktasi)
                        .accessDeniedHandler(erisimReddiIsleyicisi));
        return http.build();
    }
}
