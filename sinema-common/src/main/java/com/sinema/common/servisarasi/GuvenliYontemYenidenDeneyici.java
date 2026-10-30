package com.sinema.common.servisarasi;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;

/**
 * Feign yeniden denemesini yalnızca GET isteklerine uygular (planlama M3-1: "retry yalnızca GET").
 * Feign, ağ hatasında (bağlantı kurulamadı, okuma zaman aşımı) RetryableException fırlatır; POST'ta istek karşı
 * tarafa ulaşmış ve işlenmiş olabilir, tekrar göndermek işi iki kez yaptırabilir. GET güvenlidir.
 *
 * Decorator: bekleme/deneme sayısı mantığı Feign'in Retryer.Default'unda kalır, burada yalnızca filtre var.
 */
public class GuvenliYontemYenidenDeneyici implements Retryer {

    private final Retryer temel;

    public GuvenliYontemYenidenDeneyici(Retryer temel) {
        this.temel = temel;
    }

    @Override
    public void continueOrPropagate(RetryableException hata) {
        if (hata.method() != Request.HttpMethod.GET) {
            throw hata;
        }
        temel.continueOrPropagate(hata);
    }

    /** Feign her çağrı için kopya alır: deneme sayacı çağrıya özeldir. */
    @Override
    public Retryer clone() {
        return new GuvenliYontemYenidenDeneyici(temel.clone());
    }
}
