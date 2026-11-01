package com.sinema.booking.goc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Bir salon düzeni göçü (layout_migrations, K35). Salon ve düzenler catalog'a yumuşak referanstır. */
@Entity
@Table(name = "layout_migrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DuzenGocu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hall_id", nullable = false, updatable = false)
    private Long salonId;

    @Column(name = "to_layout_id", nullable = false, updatable = false)
    private Long hedefDuzenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GocDurumu durum;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long baslatanKullaniciId;

    @Column(name = "failure_code", length = 50)
    private String hataKodu;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    @Column(name = "completed_at")
    private Instant tamamlanmaZamani;

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof DuzenGocu gocu && id != null && id.equals(gocu.id);
    }

    @Override
    public int hashCode() {
        return DuzenGocu.class.hashCode();
    }
}
