package com.sinema.booking.goc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Salon bakım kilidi (hall_maintenance_locks): satır varken salonun seanslarında yeni koltuk tutulamaz. Kimlik
 * salon kimliğidir; PRIMARY KEY aynı salonda aynı anda ikinci göçü DB düzeyinde engeller.
 */
@Entity
@Table(name = "hall_maintenance_locks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BakimKilidi {

    @Id
    @Column(name = "hall_id")
    private Long salonId;

    @Column(name = "migration_id", nullable = false, updatable = false)
    private Long gocId;

    @Column(name = "locked_at", nullable = false, updatable = false)
    private Instant kilitlenmeZamani;

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof BakimKilidi kilit && salonId != null && salonId.equals(kilit.salonId);
    }

    @Override
    public int hashCode() {
        return BakimKilidi.class.hashCode();
    }
}
