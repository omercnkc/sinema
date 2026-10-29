package com.sinema.common.yapilandirma;

import com.sinema.common.guvenlik.GuvenlikHataYoneticisi;
import com.sinema.common.guvenlik.ProblemJsonErisimReddiIsleyicisi;
import com.sinema.common.guvenlik.ProblemJsonGirisNoktasi;
import com.sinema.common.guvenlik.ProblemJsonYazici;
import com.sinema.common.hata.GlobalHataYoneticisi;
import com.sinema.common.korelasyon.KorelasyonKimligiFiltresi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * sinema-common'ı bağımlılık olarak ekleyen servlet servisine ortak bean'leri otomatik kaydeder;
 * servislerin her birinde aynı @Bean tanımını tekrar yazmaya gerek kalmaz.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SinemaCommonOtomatikYapilandirma {

    @Bean
    @ConditionalOnMissingBean
    public KorelasyonKimligiFiltresi korelasyonKimligiFiltresi() {
        return new KorelasyonKimligiFiltresi();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalHataYoneticisi globalHataYoneticisi() {
        return new GlobalHataYoneticisi();
    }

    /** Yalnızca Spring Security olan serviste: 401/403 cevapları da ortak hata biçiminde yazılır. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(AuthenticationEntryPoint.class)
    static class GuvenlikHataYapilandirmasi {

        @Bean
        @ConditionalOnMissingBean
        public ProblemJsonYazici problemJsonYazici() {
            return new ProblemJsonYazici();
        }

        @Bean
        @ConditionalOnMissingBean
        public ProblemJsonGirisNoktasi problemJsonGirisNoktasi(ProblemJsonYazici yazici) {
            return new ProblemJsonGirisNoktasi(yazici);
        }

        @Bean
        @ConditionalOnMissingBean
        public ProblemJsonErisimReddiIsleyicisi problemJsonErisimReddiIsleyicisi(ProblemJsonYazici yazici) {
            return new ProblemJsonErisimReddiIsleyicisi(yazici);
        }

        @Bean
        @ConditionalOnMissingBean
        public GuvenlikHataYoneticisi guvenlikHataYoneticisi() {
            return new GuvenlikHataYoneticisi();
        }
    }
}
