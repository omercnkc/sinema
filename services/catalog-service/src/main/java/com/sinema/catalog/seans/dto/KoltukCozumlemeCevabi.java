package com.sinema.catalog.seans.dto;

import com.sinema.catalog.duzen.entity.KoltukTipi;
import java.util.List;

/**
 * seats: seansın mühürlü düzeninde gerçekten koltuk olan hücreler, etiketleriyle (bilet snapshot'ı, ör. "F-7").
 * invalidCellIds: düzende olmayan ya da koltuk olmayan (perde, duvar...) hücreler. Karar booking'indir:
 * biri bile geçersizse hold açılmaz (§4.3). Hata yerine liste dönülür ki booking hangilerinin hatalı olduğunu bilsin.
 */
public record KoltukCozumlemeCevabi(Long layoutId, List<Koltuk> seats, List<Long> invalidCellIds) {

    public record Koltuk(Long cellId, String seatRowLabel, int seatNumber, String label, KoltukTipi seatType) {
    }
}
