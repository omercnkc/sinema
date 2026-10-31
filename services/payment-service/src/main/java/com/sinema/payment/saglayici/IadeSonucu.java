package com.sinema.payment.saglayici;

/** İade sonucu; başarısızsa hata kodu mutabakat job'ında yeniden denemeye karar vermek için saklanır. */
public record IadeSonucu(boolean basarili, String hataKodu) {

    public static IadeSonucu iadeEdildi() {
        return new IadeSonucu(true, null);
    }

    public static IadeSonucu reddedildi(String hataKodu) {
        return new IadeSonucu(false, hataKodu);
    }
}
