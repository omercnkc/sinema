package com.sinema.common.servisarasi;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;

/**
 * Her Feign isteğine servis JWT'sini ekler. Token'ın "aud" değeri Feign istemcisinin adıdır
 * (@FeignClient(name = "catalog-service")), bu yüzden istemci adı çağrılan servisin spring.application.name'i olmalı.
 * Kullanıcı JWT'si iletilmez (planlama §5.5): iç uçlar kullanıcıya değil servise güvenir.
 */
public class ServisTokeniEkleyici implements RequestInterceptor {

    private static final String BEARER_ON_EKI = "Bearer ";

    private final ServisTokeniSaglayici tokenSaglayici;

    public ServisTokeniEkleyici(ServisTokeniSaglayici tokenSaglayici) {
        this.tokenSaglayici = tokenSaglayici;
    }

    @Override
    public void apply(RequestTemplate istek) {
        String hedefServis = istek.feignTarget().name();
        istek.header(HttpHeaders.AUTHORIZATION, BEARER_ON_EKI + tokenSaglayici.token(hedefServis));
    }
}
