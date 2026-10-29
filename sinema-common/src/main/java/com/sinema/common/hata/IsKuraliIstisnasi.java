package com.sinema.common.hata;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** 422: istek biçimce doğru ama bir iş kuralını ihlal ediyor (ör. satış kapandı). */
public class IsKuraliIstisnasi extends SinemaIstisnasi {

    public IsKuraliIstisnasi(String kod, String mesaj) {
        super(kod, mesaj, Map.of());
    }

    public IsKuraliIstisnasi(String kod, String mesaj, Map<String, Object> ekAlanlar) {
        super(kod, mesaj, ekAlanlar);
    }

    @Override
    public HttpStatus getDurum() {
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }
}
