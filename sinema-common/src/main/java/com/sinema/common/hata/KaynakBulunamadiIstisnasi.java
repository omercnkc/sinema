package com.sinema.common.hata;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * 404: kayıt yok ya da isteyen kişiye ait değil. İkisi bilerek ayırt edilmez,
 * böylece başkasının kaydının var olduğu sızdırılmaz (planlama §7.2).
 */
public class KaynakBulunamadiIstisnasi extends SinemaIstisnasi {

    public KaynakBulunamadiIstisnasi(String kod, String mesaj) {
        super(kod, mesaj, Map.of());
    }

    @Override
    public HttpStatus getDurum() {
        return HttpStatus.NOT_FOUND;
    }
}
