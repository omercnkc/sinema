package com.sinema.common.hata;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** 403: kim olduğu belli ama bu işleme izni yok (ör. servis, izin verilmeyen bir hedef için token istedi). */
public class ErisimReddiIstisnasi extends SinemaIstisnasi {

    public ErisimReddiIstisnasi(String kod, String mesaj) {
        super(kod, mesaj, Map.of());
    }

    @Override
    public HttpStatus getDurum() {
        return HttpStatus.FORBIDDEN;
    }
}
