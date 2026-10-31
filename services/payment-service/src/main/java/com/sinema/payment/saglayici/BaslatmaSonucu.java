package com.sinema.payment.saglayici;

import java.net.URI;
import java.time.Instant;

/**
 * @param token          ödeme sayfasının kimliği; callback'te geri gelir
 * @param tokenBitisi    bu andan sonra sayfa kullanılamaz (iyzico: 30 dk)
 * @param odemeSayfasi   kullanıcının yönlendirildiği adres (tam sayfa, iframe yok, K41)
 */
public record BaslatmaSonucu(String token, Instant tokenBitisi, URI odemeSayfasi) {
}
