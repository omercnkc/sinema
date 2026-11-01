package com.sinema.booking.katalog.istemci;

import java.time.Instant;
import java.util.List;

/**
 * catalog'un göç kaynağı cevabı (GET /internal/v1/catalog/halls/{id}/migration-source): hedef taslak, salonun
 * başlamamış seansları ve bu seansların kullandığı düzen sürümleri. Feign DTO'su, catalog'un iç modelinden ayrı.
 */
public record KatalogGocKaynagi(
        Long hallId,
        DuzenHucreleri targetLayout,
        List<Seans> showtimes,
        List<DuzenHucreleri> sourceLayouts) {

    public record Seans(Long id, Instant startsAt, Long layoutId) {
    }

    public record DuzenHucreleri(Long layoutId, int gridRows, int gridCols, List<Hucre> cells) {
    }

    /** {@code type}: SEAT, SCREEN, ENTRANCE, EXIT, WALL; koltuk alanları yalnızca SEAT'te dolu. */
    public record Hucre(Long cellId, int row, int col, String type, String seatRowLabel, Integer seatNumber,
            String seatType) {
    }
}
