package com.sinema.booking.koltuk.entity;

/**
 * Seanstaki bir koltuğun durumu (planlama §2.3). Satır yoksa ya da HELD süresi dolmuşsa koltuk boştur.
 * SOLD son durumdur: iptal/iade yok (K6).
 */
public enum KoltukDurumu {
    HELD,
    SOLD
}
