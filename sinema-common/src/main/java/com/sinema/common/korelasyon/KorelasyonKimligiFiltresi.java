package com.sinema.common.korelasyon;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Her isteğe bir korelasyon kimliği bağlar: gelen başlık geçerliyse onu kullanır, değilse yenisini üretir.
 * Kimlik MDC'ye yazılır (her log satırına eklenir) ve cevap başlığında geri döner.
 */
public class KorelasyonKimligiFiltresi extends OncePerRequestFilter implements Ordered {

    // Dışarıdan gelen değer loglara yazıldığı için sınırlanır: log enjeksiyonu ve aşırı uzun değer engellenir
    private static final Pattern GECERLI_KIMLIK = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest istek, HttpServletResponse cevap, FilterChain zincir)
            throws ServletException, IOException {
        String kimlik = gecerliKimlikVeyaYeni(istek.getHeader(KorelasyonKimligi.HTTP_BASLIGI));
        MDC.put(KorelasyonKimligi.MDC_ANAHTARI, kimlik);
        cevap.setHeader(KorelasyonKimligi.HTTP_BASLIGI, kimlik);
        try {
            zincir.doFilter(istek, cevap);
        } finally {
            // Thread havuzdaki bir sonraki isteğe eski kimlik sızmasın
            MDC.remove(KorelasyonKimligi.MDC_ANAHTARI);
        }
    }

    private static String gecerliKimlikVeyaYeni(String gelenKimlik) {
        if (gelenKimlik != null && GECERLI_KIMLIK.matcher(gelenKimlik).matches()) {
            return gelenKimlik;
        }
        return UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        // Güvenlik dahil tüm filtrelerden önce çalışır; yetki hataları da kimlikle loglanır
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
