package com.sinema.booking.rezervasyon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.sinema.booking.katalog.istemci.KatalogIstemcisi;
import com.sinema.booking.katalog.istemci.KatalogSeansBilgisi;
import com.sinema.booking.katalog.istemci.KoltukCozumlemeIstegi;
import com.sinema.booking.katalog.istemci.KoltukCozumlemeSonucu;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Rezervasyon entegrasyon testlerinin ortak tabanı: gerçek MySQL, sahte catalog. Alt sınıflar aynı Spring
 * context'ini paylaşır. Her test kendi seans id'sini alır, böylece testler birbirinin satırlarını görmez ve
 * tablo temizliği gerekmez.
 *
 * Sahte catalog: her seans satışta (kapanış 2 saat sonra, fiyat 150.00); seansGecersizHucreleri'ne eklenen
 * hücreler dışında istenen her hücre koltuktur, etiketi "A-{hücre id}".
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MysqlTestYapilandirmasi.class)
public abstract class RezervasyonEntegrasyonTesti {

    protected static final BigDecimal BILET_FIYATI = new BigDecimal("150.00");
    private static final AtomicLong SEANS_SAYACI = new AtomicLong(System.currentTimeMillis() % 1_000_000 * 1_000);

    @MockitoBean
    protected KatalogIstemcisi katalogIstemcisi;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected final Map<Long, Instant> satisKapanislari = new ConcurrentHashMap<>();
    protected final Set<Long> gecersizHucreler = ConcurrentHashMap.newKeySet();

    @BeforeEach
    void sahteKatalogKur() {
        when(katalogIstemcisi.seansBilgisi(anyLong())).thenAnswer(cagri -> seansBilgisi(cagri.getArgument(0)));
        when(katalogIstemcisi.koltuklariCoz(anyLong(), any())).thenAnswer(cagri -> koltukCozumu(cagri.getArgument(1)));
    }

    protected static long yeniSeansId() {
        return SEANS_SAYACI.incrementAndGet();
    }

    /** Başka bir kullanıcının, süresi dolmuş ama job'ın henüz temizlemediği tutmasını DB'ye yazar. */
    protected long suresiDolmusTutmaEkle(long seansId, long hucreId, long kullaniciId) {
        Timestamp gecmis = Timestamp.from(Instant.now().minus(Duration.ofMinutes(1)));
        long rezervasyonId = bekleyenRezervasyonEkle(seansId, kullaniciId, gecmis);
        jdbcTemplate.update("""
                INSERT INTO seat_bookings (showtime_id, layout_cell_id, status, hold_expires_at, user_id,
                                           reservation_id, version, created_at, updated_at)
                VALUES (?, ?, 'HELD', ?, ?, ?, 0, ?, ?)""",
                seansId, hucreId, gecmis, kullaniciId, rezervasyonId, gecmis, gecmis);
        return rezervasyonId;
    }

    protected long bekleyenRezervasyonEkle(long seansId, long kullaniciId, Timestamp bitis) {
        jdbcTemplate.update("""
                INSERT INTO reservations (user_id, showtime_id, hall_id, status, total_amount, expires_at,
                                          movie_title, cinema_name, hall_name, starts_at, created_at, updated_at)
                VALUES (?, ?, 1, 'PENDING_PAYMENT', 150.00, ?, 'Film', 'Şube', 'Salon', ?, ?, ?)""",
                kullaniciId, seansId, bitis, bitis, bitis, bitis);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reservations WHERE user_id = ? AND showtime_id = ? ORDER BY id DESC LIMIT 1",
                Long.class, kullaniciId, seansId);
    }

    protected String rezervasyonDurumu(long rezervasyonId) {
        return jdbcTemplate.queryForObject("SELECT status FROM reservations WHERE id = ?", String.class,
                rezervasyonId);
    }

    /** Seansta koltuğun sahibi olan rezervasyon (satır yoksa null). */
    protected Long koltukSahibi(long seansId, long hucreId) {
        List<Long> sahip = jdbcTemplate.queryForList(
                "SELECT reservation_id FROM seat_bookings WHERE showtime_id = ? AND layout_cell_id = ?",
                Long.class, seansId, hucreId);
        return sahip.isEmpty() ? null : sahip.getFirst();
    }

    protected int seansinRezervasyonSayisi(long seansId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservations WHERE showtime_id = ?",
                Integer.class, seansId);
    }

    private KatalogSeansBilgisi seansBilgisi(long seansId) {
        Instant baslangic = Instant.now().plus(Duration.ofHours(2));
        Instant kapanis = satisKapanislari.getOrDefault(seansId, baslangic.plus(Duration.ofMinutes(10)));
        return new KatalogSeansBilgisi(seansId, 3L, "Örnek Film", 1L, "Kadıköy", 2L, "Salon 2", 5L,
                baslangic, baslangic.plus(Duration.ofMinutes(135)), kapanis, BILET_FIYATI);
    }

    private KoltukCozumlemeSonucu koltukCozumu(KoltukCozumlemeIstegi istek) {
        List<KoltukCozumlemeSonucu.Koltuk> koltuklar = istek.cellIds().stream()
                .filter(hucre -> !gecersizHucreler.contains(hucre))
                .map(hucre -> new KoltukCozumlemeSonucu.Koltuk(hucre, "A", hucre.intValue(), "A-" + hucre, "STANDARD"))
                .toList();
        List<Long> gecersiz = istek.cellIds().stream().filter(gecersizHucreler::contains).toList();
        return new KoltukCozumlemeSonucu(5L, koltuklar, gecersiz);
    }
}
