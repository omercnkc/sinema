package com.sinema.common.guvenlik;

import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * /internal/** yolları için (planlama §5.5): yalnızca SERVICE rolündeki ve "aud" alanında BU servisin adı
 * bulunan token geçer. Böylece booking için alınmış bir servis token'ı catalog'un iç uçlarında kullanılamaz;
 * kullanıcı token'ı (USER/ADMIN) ise hiçbir iç uca giremez.
 */
public class ServisErisimDenetcisi implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String SERVIS_YETKISI = "ROLE_" + Roller.SERVICE;

    private final String buServisinAdi;

    public ServisErisimDenetcisi(String buServisinAdi) {
        this.buServisinAdi = buServisinAdi;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> kimlik, RequestAuthorizationContext baglam) {
        return new AuthorizationDecision(izinliMi(kimlik.get()));
    }

    private boolean izinliMi(Authentication kimlik) {
        if (!(kimlik instanceof JwtAuthenticationToken jwtKimligi)) {
            return false;
        }
        boolean servisRolunde = jwtKimligi.getAuthorities().stream()
                .anyMatch(yetki -> SERVIS_YETKISI.equals(yetki.getAuthority()));
        return servisRolunde && jwtKimligi.getToken().getAudience().contains(buServisinAdi);
    }
}
