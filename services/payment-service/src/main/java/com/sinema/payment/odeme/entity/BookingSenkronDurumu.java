package com.sinema.payment.odeme.entity;

/**
 * Ödeme sonucunun booking'e iletilmesi (confirm/fail, §4.8). Sonuç alındığında PENDING olur; booking'e ulaşılamazsa
 * PENDING kalır ve mutabakat job'ı yeniden dener. NOT_NEEDED: henüz iletilecek sonuç yok.
 */
public enum BookingSenkronDurumu {
    NOT_NEEDED,
    PENDING,
    DONE,
    FAILED
}
