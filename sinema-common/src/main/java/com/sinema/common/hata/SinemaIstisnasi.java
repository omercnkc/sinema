package com.sinema.common.hata;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Servislerin bilinçli olarak fırlattığı tüm hataların atası. Global hata yöneticisi bu tipi
 * tanıyıp HTTP durumunu, makine okunur kodu ve ek alanları cevaba yazar.
 * Mesaj kullanıcıya gösterilebilir olmalıdır: iç ayrıntı (SQL, sınıf adı, stack) içermez.
 */
public abstract class SinemaIstisnasi extends RuntimeException {

    private final String kod;
    // Hata cevabına eklenecek bağlama özel alanlar (ör. unavailableCellIds); null değer kabul etmez
    private final transient Map<String, Object> ekAlanlar;

    protected SinemaIstisnasi(String kod, String mesaj, Map<String, Object> ekAlanlar) {
        super(mesaj);
        this.kod = kod;
        this.ekAlanlar = Map.copyOf(ekAlanlar);
    }

    public abstract HttpStatus getDurum();

    public String getKod() {
        return kod;
    }

    public Map<String, Object> getEkAlanlar() {
        return ekAlanlar;
    }
}
