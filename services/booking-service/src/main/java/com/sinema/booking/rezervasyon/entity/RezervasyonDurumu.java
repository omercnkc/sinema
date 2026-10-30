package com.sinema.booking.rezervasyon.entity;

/**
 * Rezervasyon durum makinesi (planlama §2.3):
 * PENDING_PAYMENT → CONFIRMED (ödeme başarılı) | EXPIRED (süre doldu) | FAILED (ödeme reddedildi)
 * | RELEASED (kullanıcı bıraktı, K40). PENDING_PAYMENT dışındaki her durum son durumdur.
 */
public enum RezervasyonDurumu {
    PENDING_PAYMENT,
    CONFIRMED,
    EXPIRED,
    FAILED,
    RELEASED
}
