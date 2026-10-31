package com.sinema.payment.kullanici.istemci;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * auth-service'in alıcı bilgisi iç ucu (K48): iyzico'ya gerçek ad/soyad/e-posta/telefon gönderilir. Yalnızca okuma;
 * auth kimseyi çağırmaz, servisler arası döngü yok. auth'a ulaşılamazsa 503 AUTH_UNAVAILABLE (ödeme başlatılmaz).
 */
@FeignClient(name = "auth-service", url = "${sinema.servis-istemcisi.auth-adresi}",
        fallbackFactory = KullaniciIstemcisiYedegi.class)
public interface KullaniciIstemcisi {

    @GetMapping("/internal/v1/auth/users/{kullaniciId}/billing-info")
    AliciBilgisi aliciBilgisi(@PathVariable("kullaniciId") Long kullaniciId);
}
