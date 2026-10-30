package com.sinema.catalog.seans.dto;

import com.sinema.catalog.duzen.dto.HucreCevabi;
import java.util.List;

/**
 * Seansın mühürlü düzeni (koltuk haritası). Doluluk burada yok: booking'in seat-status ucundan gelir,
 * istemci ikisini birleştirir (§3.4-2). cellId, koltuk tutarken booking'e gönderilen kimliktir.
 */
public record SeansDuzeni(Long showtimeId, Long layoutId, int version, int gridRows, int gridCols,
        List<HucreCevabi> cells) {
}
