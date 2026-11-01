package com.sinema.catalog.duzen.dto;

import java.time.Instant;
import java.util.List;

/**
 * Booking'in bilet göçünü planlamak için okuduğu veri (K35, §7.8): hedef taslak, salonun başlamamış seansları ve
 * bu seansların şu an kullandığı düzen sürümleri (biletlerin eski konumları bunlardan çıkar). Yalnızca servis
 * JWT'siyle (/internal) okunur.
 */
public record GocKaynagi(
        Long hallId,
        DuzenHucreleri targetLayout,
        List<GocSeansi> showtimes,
        List<DuzenHucreleri> sourceLayouts) {

    public record GocSeansi(Long id, Instant startsAt, Long layoutId) {
    }

    /** Hücre alanları halka açık düzen ucuyla aynı (konum, tip, koltuk etiketi). */
    public record DuzenHucreleri(Long layoutId, int gridRows, int gridCols, List<HucreCevabi> cells) {
    }
}
