package com.sinema.catalog.duzen.dto;

import com.sinema.catalog.duzen.entity.DuzenDurumu;
import java.time.Instant;

/** Sürüm listesindeki satır: hücreler olmadan, koltuk sayısıyla. */
public record DuzenOzeti(
        Long id,
        Integer version,
        DuzenDurumu status,
        boolean active,
        int gridRows,
        int gridCols,
        long seatCount,
        Instant createdAt,
        Instant publishedAt) {
}
