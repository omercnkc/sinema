package com.sinema.booking.koltuk.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * K9'un çekirdeği: koltuğu tek atomik SQL ifadesiyle alır (planlama §4.2-B, §4.3). "Boş mu bak, sonra yaz" yok;
 * kararı her zaman UNIQUE(showtime_id, layout_cell_id) kısıtı ve koşullu UPDATE verir.
 *
 * JPA yerine JDBC: JPA'da başarısız INSERT (benzersizlik hatası) transaction'ı geri alınacak diye işaretler.
 * Burada o hata beklenen bir sonuçtur ("koltuk dolu"), yakalanıp karara çevrilir; transaction'ı çağıran yönetir.
 * Çağıran transaction READ COMMITTED olmalı (KoltukTutmaIslemi): gap lock olmadığı için eşzamanlı tutmalar
 * birbirini kilitlenmeye (deadlock) sokmaz.
 */
@Repository
@RequiredArgsConstructor
public class KoltukKilitDeposu {

    private static final String DEVRAL = """
            UPDATE seat_bookings
               SET status = 'HELD', hold_expires_at = :tutmaBitisi, user_id = :kullaniciId,
                   reservation_id = :rezervasyonId, version = version + 1, updated_at = :simdi
             WHERE showtime_id = :seansId AND layout_cell_id = :hucreId
               AND status = 'HELD' AND hold_expires_at <= :simdi""";

    private static final String EKLE = """
            INSERT INTO seat_bookings (showtime_id, layout_cell_id, status, hold_expires_at, user_id, reservation_id,
                                       version, created_at, updated_at)
            VALUES (:seansId, :hucreId, 'HELD', :tutmaBitisi, :kullaniciId, :rezervasyonId, 0, :simdi, :simdi)""";

    private static final String DOLU_HUCRELER = """
            SELECT layout_cell_id FROM seat_bookings
             WHERE showtime_id = :seansId AND layout_cell_id IN (:hucreIdleri)
               AND (status = 'SOLD' OR hold_expires_at > :simdi)
             ORDER BY layout_cell_id""";

    // Yalnızca bu rezervasyonun tutmaları: süresi dolup başkasının devraldığı satırın reservation_id'si artık farklıdır
    private static final String TUTMALARI_SIL = """
            DELETE FROM seat_bookings WHERE reservation_id = :rezervasyonId AND status = 'HELD'""";

    // Satış: yalnızca bu rezervasyonun hâlâ geçerli tutmaları (§4.5). Süresi dolmuş tutma satılmaz.
    private static final String SAT = """
            UPDATE seat_bookings
               SET status = 'SOLD', hold_expires_at = NULL, version = version + 1, updated_at = :simdi
             WHERE reservation_id = :rezervasyonId AND status = 'HELD' AND hold_expires_at > :simdi""";

    private static final String TUTMA_BITISINI_GUNCELLE = """
            UPDATE seat_bookings
               SET hold_expires_at = :bitis, version = version + 1, updated_at = :simdi
             WHERE reservation_id = :rezervasyonId AND status = 'HELD' AND hold_expires_at > :simdi""";

    private static final String SURESI_DOLMUSLARI_SIL = """
            DELETE FROM seat_bookings WHERE status = 'HELD' AND hold_expires_at <= :simdi LIMIT :parti""";

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Koltuğu bu rezervasyon adına tutar; alınamadıysa false (satılmış ya da süresi dolmamış başka tutma var).
     *
     * Önce devralma denenir: satır varsa ve süresi dolmuş bir tutmaysa tek UPDATE ile alınır. İki kişi aynı süresi
     * dolmuş satırı devralmaya çalışırsa ikincisinin UPDATE'i birincinin commit'ini bekler, sonra koşulu yeniden
     * değerlendirir (artık süresi dolmamış) ve 0 satır döner. Satır yoksa INSERT; aynı anda ekleyen olursa
     * benzersizlik kısıtı ikincisini reddeder.
     */
    public boolean tut(KoltukTutma tutma) {
        MapSqlParameterSource parametreler = parametreler(tutma);
        if (jdbc.update(DEVRAL, parametreler) == 1) {
            return true;
        }
        try {
            jdbc.update(EKLE, parametreler);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Rezervasyonun tutmalarını siler; koltuklar hemen boşalır (bırakma, K40). SOLD satırlara dokunmaz (K6). */
    public int tutmalariSil(Long rezervasyonId) {
        return jdbc.update(TUTMALARI_SIL, new MapSqlParameterSource("rezervasyonId", rezervasyonId));
    }

    /** HELD → SOLD; dönen sayı rezervasyonun koltuk sayısına eşit değilse çağıran işlemi geri almalı. */
    public int sat(Long rezervasyonId, Instant simdi) {
        return jdbc.update(SAT, rezervasyonParametreleri(rezervasyonId, simdi));
    }

    /** Ödeme penceresi uzatmasında koltuk tutmaları rezervasyonla aynı bitişe çekilir (K32). */
    public int tutmaBitisiniGuncelle(Long rezervasyonId, Instant bitis, Instant simdi) {
        return jdbc.update(TUTMA_BITISINI_GUNCELLE,
                rezervasyonParametreleri(rezervasyonId, simdi).addValue("bitis", Timestamp.from(bitis)));
    }

    /**
     * Süre dolumu job'ı: süresi dolmuş tutma satırlarını siler (K43), en çok {@code parti} satır. Koşul aynı
     * ifadede olduğu için arada devralınmış (bitişi ileri alınmış) satır silinmez.
     */
    public int suresiDolmuslariSil(Instant simdi, int parti) {
        return jdbc.update(SURESI_DOLMUSLARI_SIL, new MapSqlParameterSource()
                .addValue("simdi", Timestamp.from(simdi))
                .addValue("parti", parti));
    }

    /** Verilen hücrelerden şu an dolu olanlar (409 cevabında hangi koltukların alındığını göstermek için). */
    public List<Long> doluHucreler(Long seansId, Collection<Long> hucreIdleri, Instant simdi) {
        if (hucreIdleri.isEmpty()) {
            return List.of();
        }
        return jdbc.queryForList(DOLU_HUCRELER, new MapSqlParameterSource()
                .addValue("seansId", seansId)
                .addValue("hucreIdleri", hucreIdleri)
                .addValue("simdi", Timestamp.from(simdi)), Long.class);
    }

    private static MapSqlParameterSource rezervasyonParametreleri(Long rezervasyonId, Instant simdi) {
        return new MapSqlParameterSource()
                .addValue("rezervasyonId", rezervasyonId)
                .addValue("simdi", Timestamp.from(simdi));
    }

    private static MapSqlParameterSource parametreler(KoltukTutma tutma) {
        return new MapSqlParameterSource()
                .addValue("seansId", tutma.seansId())
                .addValue("hucreId", tutma.hucreId())
                .addValue("kullaniciId", tutma.kullaniciId())
                .addValue("rezervasyonId", tutma.rezervasyonId())
                .addValue("tutmaBitisi", Timestamp.from(tutma.tutmaBitisi()))
                .addValue("simdi", Timestamp.from(tutma.simdi()));
    }

    /** Tek koltuğun tutma isteği; simdi tüm karşılaştırmalarda kullanılan tek zaman kaynağıdır (§2.4-10). */
    public record KoltukTutma(Long seansId, Long hucreId, Long kullaniciId, Long rezervasyonId, Instant tutmaBitisi,
            Instant simdi) {
    }
}
