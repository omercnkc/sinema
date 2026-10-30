package com.sinema.booking.config;

import com.sinema.booking.RezervasyonServisiUygulamasi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Feign istemcilerini servisin tüm paketlerinde arar. Token, korelasyon kimliği, hata çözümü ve devre kesici
 * sinema-common'ın SinemaServisIstemcisiOtomatikYapilandirma'sından gelir (sinema.servis-istemcisi.*).
 */
@Configuration
@EnableFeignClients(basePackageClasses = RezervasyonServisiUygulamasi.class)
public class FeignYapilandirmasi {
}
