package com.sinema.booking.goc.dto;

import com.sinema.booking.goc.entity.GocNedeni;
import java.time.Instant;
import java.util.List;

/**
 * Göç önizlemesi (K35): taslak yayımlanırsa biletlere ne olacağı. Aynı konum ve etiketinde kalanlar yalnızca
 * sayılır; etiketi değişen ve yedek atanan biletler listelenir (sahiplerine bildirim gider, K51). Çözülemeyen
 * rezervasyon varsa {@code applicable} false: göç uygulanmaz, düzen düzeltilmeli.
 *
 * @param showtimes yalnızca bileti olan başlamamış seanslar; biletsizler de taşınır ({@code summary})
 */
public record GocRaporu(Long hallId, Long toLayoutId, boolean applicable, Ozet summary, List<SeansRaporu> showtimes) {

    /**
     * @param migratedShowtimes yeni sürüme geçecek başlamamış seans sayısı (biletsizler dahil)
     * @param unresolvedReservations yeri bulunamayan rezervasyon sayısı
     */
    public record Ozet(int migratedShowtimes, int seats, int unchanged, int labelChanged, int replaced,
            int unresolvedReservations) {
    }

    public record SeansRaporu(Long showtimeId, Instant startsAt, int unchanged, List<Degisiklik> changes,
            List<Cozulemeyen> unresolved) {
    }

    public record Degisiklik(Long reservationId, String oldLabel, String newLabel, GocNedeni reason) {
    }

    public record Cozulemeyen(Long reservationId, List<String> oldLabels) {
    }
}
