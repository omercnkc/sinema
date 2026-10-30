package com.sinema.common.servisarasi;

import com.sinema.common.korelasyon.KorelasyonKimligi;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;

/**
 * Gelen isteğin korelasyon kimliğini (KorelasyonKimligiFiltresi MDC'ye yazar) çağrılan servise taşır;
 * böylece tek kullanıcı isteği tüm servislerin loglarında aynı kimlikle izlenir (rules §5.7).
 * Zamanlanmış job gibi istek dışı çağrılarda kimlik yoktur; çağrılan servis kendisi üretir.
 */
public class KorelasyonKimligiEkleyici implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate istek) {
        String kimlik = MDC.get(KorelasyonKimligi.MDC_ANAHTARI);
        if (kimlik != null) {
            istek.header(KorelasyonKimligi.HTTP_BASLIGI, kimlik);
        }
    }
}
