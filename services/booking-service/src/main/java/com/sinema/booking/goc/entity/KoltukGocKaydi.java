package com.sinema.booking.goc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.sinema.booking.goc.plan.KoltukTasima;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Göçte yeri değişen bir koltuğun denetim kaydı (seat_migration_audit). SOLD satırın değişmezliğinin tek istisnası
 * budur (K35): her değişiklik burada iz bırakır; telafi eski hücre ve etiketi buradan geri yazar.
 */
@Entity
@Table(name = "seat_migration_audit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KoltukGocKaydi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "migration_id", nullable = false, updatable = false)
    private Long gocId;

    @Column(name = "reservation_id", nullable = false, updatable = false)
    private Long rezervasyonId;

    @Column(name = "showtime_id", nullable = false, updatable = false)
    private Long seansId;

    @Column(name = "old_layout_cell_id", nullable = false, updatable = false)
    private Long eskiHucreId;

    @Column(name = "new_layout_cell_id", nullable = false, updatable = false)
    private Long yeniHucreId;

    @Column(name = "old_seat_label", nullable = false, updatable = false, length = 10)
    private String eskiEtiket;

    @Column(name = "new_seat_label", nullable = false, updatable = false, length = 10)
    private String yeniEtiket;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, updatable = false, length = 20)
    private GocNedeni neden;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    /**
     * @param biletEtiketi taşımadan önce biletin üzerinde yazan etiket (eski düzenin etiketiyle aynı olmalı; telafi
     *                     ve bildirim biletin gerçekte taşıdığı değeri kullanır)
     */
    public static KoltukGocKaydi olustur(Long gocId, Long seansId, KoltukTasima tasima, String biletEtiketi,
            Instant simdi) {
        KoltukGocKaydi kayit = new KoltukGocKaydi();
        kayit.gocId = gocId;
        kayit.rezervasyonId = tasima.rezervasyonId();
        kayit.seansId = seansId;
        kayit.eskiHucreId = tasima.eskiHucreId();
        kayit.yeniHucreId = tasima.yeniHucreId();
        kayit.eskiEtiket = biletEtiketi;
        kayit.yeniEtiket = tasima.yeniEtiket();
        kayit.neden = tasima.neden();
        kayit.olusturulmaZamani = simdi;
        return kayit;
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof KoltukGocKaydi kayit && id != null && id.equals(kayit.id);
    }

    @Override
    public int hashCode() {
        return KoltukGocKaydi.class.hashCode();
    }
}
