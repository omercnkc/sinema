package com.sinema.booking.doluluk.dto;

import com.sinema.booking.koltuk.entity.KoltukDurumu;

/**
 * Doluluk listesindeki bir koltuk. Ayrı (üst düzey) record: JPQL "select new" ifadesi iç içe sınıf adını çözemiyor
 * ve repository bu tipi doğrudan üretir.
 */
public record DoluKoltuk(Long cellId, KoltukDurumu status) {
}
