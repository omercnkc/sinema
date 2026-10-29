package com.sinema.common.yapilandirma;

import com.sinema.common.hata.GlobalHataYoneticisi;
import com.sinema.common.korelasyon.KorelasyonKimligiFiltresi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

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
}
