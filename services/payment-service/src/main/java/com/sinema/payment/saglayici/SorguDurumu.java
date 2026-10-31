package com.sinema.payment.saglayici;

/**
 * Token sorgusunun sonucu.
 * BEKLIYOR: kullanıcı henüz ödemedi (iyzico 5122) ya da sağlayıcı ödemeyi incelemede (fraudStatus 0); sonra tekrar
 * sorulur. BULUNAMADI: sağlayıcı token'ı hiç tanımıyor (iyzico 5124) → sahte ya da başka ortamın token'ı.
 */
public enum SorguDurumu {
    BASARILI,
    BASARISIZ,
    BEKLIYOR,
    BULUNAMADI
}
