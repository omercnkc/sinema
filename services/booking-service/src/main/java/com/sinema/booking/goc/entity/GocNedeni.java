package com.sinema.booking.goc.entity;

/** Bir koltuğun göçte nasıl eşlendiği (K35, K51). */
public enum GocNedeni {
    /** Yeni düzende aynı (satır, sütun) hâlâ koltuk ve etiket de aynı: sessiz güncelleme. */
    SAME_POSITION,
    /** Konum aynı ama etiket değişti (ör. sıra kaldırılınca F-7 → E-7): bildirim gider (K51). */
    LABEL_CHANGED,
    /** Koltuk kalktı; boş, yakın bir yedek koltuk atandı: bildirim gider. */
    REPLACEMENT;

    public boolean bildirimGerekir() {
        return this != SAME_POSITION;
    }
}
