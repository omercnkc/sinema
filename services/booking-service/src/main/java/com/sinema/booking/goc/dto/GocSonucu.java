package com.sinema.booking.goc.dto;

import com.sinema.booking.goc.entity.GocDurumu;

/**
 * Uygulanan göç: yayımlanan sürüm ve yeni sürüme geçen seans sayısı. {@code report} uygulamadan hemen önce
 * hesaplanan plandır; askıdaki göç sürdürüldüğünde (plan zaten uygulanmıştı) boştur.
 */
public record GocSonucu(Long migrationId, GocDurumu status, int layoutVersion, int migratedShowtimes,
        GocRaporu report) {
}
