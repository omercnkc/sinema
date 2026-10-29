package com.sinema.common.guvenlik;

import java.util.List;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Yetki kuralları, sırayla (ilk eşleşen kazanır):
 *  1. servisin açık yolları + sağlık kontrolü → herkes
 *  2. /internal/**                          → yalnızca bu servise kesilmiş SERVICE token'ı
 *  3. /api/v1/admin/**                       → ADMIN
 *  4. geri kalan her şey                     → USER veya ADMIN (servis token'ı kullanıcı uçlarına giremez)
 * Listede olmayan bir uç yanlışlıkla açık kalamaz: varsayılan kapalıdır.
 */
public final class GuvenlikKurallari {

    public static final String SAGLIK_YOLU = "/actuator/health/**";
    public static final String IC_YOLLAR = "/internal/**";
    public static final String ADMIN_YOLLARI = "/api/v1/admin/**";

    private GuvenlikKurallari() {
    }

    public static void uygula(
            AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry yetki,
            List<String> acikYollar, String servisAdi) {
        acikYollar.stream().map(AcikYol::coz).forEach(yol -> {
            if (yol.metot() == null) {
                yetki.requestMatchers(yol.desen()).permitAll();
            } else {
                yetki.requestMatchers(yol.metot(), yol.desen()).permitAll();
            }
        });
        yetki.requestMatchers(SAGLIK_YOLU).permitAll()
                .requestMatchers(IC_YOLLAR).access(new ServisErisimDenetcisi(servisAdi))
                .requestMatchers(ADMIN_YOLLARI).hasRole(Roller.ADMIN)
                .anyRequest().hasAnyRole(Roller.USER, Roller.ADMIN);
    }
}
