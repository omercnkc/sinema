package com.sinema.booking.goc.entity;

/** Göç işinin durumu. Önizleme kaydedilmez; kayıt uygulama başlayınca RUNNING olarak açılır. */
public enum GocDurumu {
    /** Biletler taşındı (commit), catalog `activate` bekleniyor ya da sürüyor; salon kilitli. */
    RUNNING,
    COMPLETED,
    /** catalog `activate` başarısız oldu; biletler denetim kaydından eski yerlerine döndü. */
    ROLLED_BACK,
    /** Telafi de tamamlanamadı; elle müdahale gerekir (kilit kalır). */
    FAILED
}
