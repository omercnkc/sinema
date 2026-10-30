package com.sinema.catalog.duzen.dto;

import com.sinema.catalog.duzen.entity.DuzenDurumu;
import java.time.Instant;
import java.util.List;

/** Düzenin tamamı (editör ve önizleme için). version, taslakta null'dır. */
public record DuzenCevabi(
        Long id,
        Long hallId,
        Integer version,
        DuzenDurumu status,
        boolean active,
        int gridRows,
        int gridCols,
        Instant createdAt,
        Instant publishedAt,
        List<HucreCevabi> cells) {
}
