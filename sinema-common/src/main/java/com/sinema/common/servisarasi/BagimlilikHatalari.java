package com.sinema.common.servisarasi;

import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.SinemaIstisnasi;

/**
 * Feign çağrısında oluşan hatanın "iş sonucu" mu (ör. 404 seans yok) yoksa "çağrılan servis çalışmıyor" mu olduğuna
 * tek yerde karar verir. Devre kesici (hangi hatalar sayılır) ve istemcilerin fallback'leri (ne fırlatılır) aynı
 * kuralı kullanır; ikisi ayrı yazılsaydı biri değişip diğeri unutulabilirdi.
 */
public final class BagimlilikHatalari {

    private BagimlilikHatalari() {
    }

    /** Bilinçli üretilmiş iş hatası (SinemaIstisnasi), ama "servis kullanılamaz" değil: devre kesici saymaz. */
    public static boolean isSonucuMu(Throwable hata) {
        return hata instanceof SinemaIstisnasi && !(hata instanceof BagimlilikKullanilamazIstisnasi);
    }

    /**
     * Fallback içinde kullanılır: iş hatası (ve token alınamadı gibi zaten çevrilmiş 503) aynen fırlatılır;
     * geri kalan her şey (ağ hatası, 5xx, açık devre) kullanıcıya genel 503 olarak döner.
     */
    public static SinemaIstisnasi cevir(Throwable hata, String kod, String mesaj) {
        if (hata instanceof SinemaIstisnasi sinemaIstisnasi) {
            return sinemaIstisnasi;
        }
        return new BagimlilikKullanilamazIstisnasi(kod, mesaj);
    }
}
