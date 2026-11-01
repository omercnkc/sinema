package com.sinema.booking.goc.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Göçün koşullu yazmaları (K35). SOLD satırın hücresi yalnızca buradan ve denetim kaydıyla değişir (db-planlama
 * §6-5). JDBC: kilit INSERT'inin benzersizlik hatası beklenen bir sonuçtur ("göç sürüyor"), JPA save() ise elle
 * atanan kimlikte var olan satırı güncellerdi.
 */
@Repository
@RequiredArgsConstructor
public class KoltukGocDeposu {

    private static final String KILITLE = """
            INSERT INTO hall_maintenance_locks (hall_id, migration_id, locked_at) VALUES (:salonId, :gocId, :simdi)""";

    private static final String KILIDI_KALDIR = """
            DELETE FROM hall_maintenance_locks WHERE hall_id = :salonId AND migration_id = :gocId""";

    // Yalnızca bu rezervasyonun satırı: arada bırakılmış/süresi dolup başkasına geçmiş koltuk taşınmaz
    private static final String KOLTUGU_TASI = """
            UPDATE seat_bookings
               SET layout_cell_id = :yeniHucreId, version = version + 1, updated_at = :simdi
             WHERE showtime_id = :seansId AND layout_cell_id = :eskiHucreId AND reservation_id = :rezervasyonId""";

    private static final String BILET_ETIKETI = """
            SELECT seat_label FROM reservation_seats WHERE reservation_id = :rezervasyonId AND layout_cell_id = :hucreId""";

    private static final String REZERVASYON_KOLTUGUNU_TASI = """
            UPDATE reservation_seats SET layout_cell_id = :yeniHucreId, seat_label = :yeniEtiket
             WHERE reservation_id = :rezervasyonId AND layout_cell_id = :eskiHucreId""";

    // Taşımadan sonra seanslarda yeni düzenin dışında hâlâ dolu koltuk kaldıysa (planlamadan sonra gelen yazma)
    private static final String ESKI_HUCREDE_KALAN = """
            SELECT COUNT(*) FROM seat_bookings
             WHERE showtime_id IN (:seansIdleri) AND layout_cell_id NOT IN (:yeniHucreIdleri)
               AND (status = 'SOLD' OR hold_expires_at > :simdi)""";

    private final NamedParameterJdbcTemplate jdbc;

    /** @throws org.springframework.dao.DuplicateKeyException salonda süren bir göç var */
    public void kilitle(Long salonId, Long gocId, Instant simdi) {
        jdbc.update(KILITLE, new MapSqlParameterSource("salonId", salonId).addValue("gocId", gocId)
                .addValue("simdi", Timestamp.from(simdi)));
    }

    public void kilidiKaldir(Long salonId, Long gocId) {
        jdbc.update(KILIDI_KALDIR, new MapSqlParameterSource("salonId", salonId).addValue("gocId", gocId));
    }

    /** @return false: koltuk bu arada boşaldı (bırakıldı ya da süresi dolup silindi), taşınacak bir şey yok */
    public boolean koltuguTasi(Long seansId, Long rezervasyonId, Long eskiHucreId, Long yeniHucreId, Instant simdi) {
        return jdbc.update(KOLTUGU_TASI, new MapSqlParameterSource("seansId", seansId)
                .addValue("rezervasyonId", rezervasyonId).addValue("eskiHucreId", eskiHucreId)
                .addValue("yeniHucreId", yeniHucreId).addValue("simdi", Timestamp.from(simdi))) == 1;
    }

    /** Biletin üzerinde yazan etiket (göçten önce okunur; telafi bileti birebir buna döndürür). */
    public String biletEtiketi(Long rezervasyonId, Long hucreId) {
        return jdbc.queryForObject(BILET_ETIKETI, new MapSqlParameterSource("rezervasyonId", rezervasyonId)
                .addValue("hucreId", hucreId), String.class);
    }

    public int rezervasyonKoltugunuTasi(Long rezervasyonId, Long eskiHucreId, Long yeniHucreId, String yeniEtiket) {
        return jdbc.update(REZERVASYON_KOLTUGUNU_TASI, new MapSqlParameterSource("rezervasyonId", rezervasyonId)
                .addValue("eskiHucreId", eskiHucreId).addValue("yeniHucreId", yeniHucreId)
                .addValue("yeniEtiket", yeniEtiket));
    }

    public int eskiHucredeKalanDoluKoltuklar(Collection<Long> seansIdleri, Collection<Long> yeniHucreIdleri,
            Instant simdi) {
        if (seansIdleri.isEmpty()) {
            return 0;
        }
        Integer sayi = jdbc.queryForObject(ESKI_HUCREDE_KALAN, new MapSqlParameterSource("seansIdleri", seansIdleri)
                .addValue("yeniHucreIdleri", yeniHucreIdleri).addValue("simdi", Timestamp.from(simdi)), Integer.class);
        return sayi == null ? 0 : sayi;
    }
}
