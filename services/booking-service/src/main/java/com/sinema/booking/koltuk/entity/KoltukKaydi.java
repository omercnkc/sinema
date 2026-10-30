package com.sinema.booking.koltuk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Seansta bir koltuğun durumu (seat_bookings): koltuk durumunun tek doğruluk kaynağı. UNIQUE(showtime_id,
 * layout_cell_id) aynı koltuğun iki kişiye verilmesini DB düzeyinde engeller (K9).
 *
 * Bu entity yalnızca okumak içindir. Tutma/devralma/satış koşullu SQL ile yapılır (KoltukKilitDeposu): "boş mu bak,
 * sonra yaz" yerine tek atomik ifade (§4.1). Bu yüzden setter ve fabrika metodu yok.
 */
@Entity
@Table(name = "seat_bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KoltukKaydi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "showtime_id", nullable = false)
    private Long seansId;

    @Column(name = "layout_cell_id", nullable = false)
    private Long hucreId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private KoltukDurumu durum;

    @Column(name = "hold_expires_at")
    private Instant tutmaBitisi;

    @Column(name = "user_id", nullable = false)
    private Long kullaniciId;

    @Column(name = "reservation_id", nullable = false)
    private Long rezervasyonId;

    @Version
    @Column(name = "version", nullable = false)
    private Long surum;

    @Column(name = "created_at", nullable = false)
    private Instant olusturulmaZamani;

    @Column(name = "updated_at", nullable = false)
    private Instant guncellenmeZamani;

    /** Koltuk şu an dolu mu: satılmış ya da süresi dolmamış tutma. */
    public boolean doluMu(Instant simdi) {
        return durum == KoltukDurumu.SOLD || tutmaBitisi.isAfter(simdi);
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof KoltukKaydi kayit && id != null && id.equals(kayit.id);
    }

    @Override
    public int hashCode() {
        return KoltukKaydi.class.hashCode();
    }
}
