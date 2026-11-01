package com.sinema.booking.goc.plan;

import com.sinema.booking.goc.entity.GocNedeni;

/** Planlanan bir koltuk değişikliği: eski hücreden yeni düzenin hücresine. */
public record KoltukTasima(long rezervasyonId, long eskiHucreId, String eskiEtiket, long yeniHucreId,
        String yeniEtiket, GocNedeni neden) {
}
