package com.sinema.catalog.duzen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Salonun bir düzen sürümü (hall_layouts). Hücreler ayrı tabloda, kendi repository'leriyle yönetilir:
 * taslak güncellenirken eski hücreleri silip yenilerini eklemek, koleksiyon üzerinden yapılınca
 * Hibernate önce INSERT çalıştırdığı için konum UNIQUE kısıtına takılırdı.
 */
@Entity
@Table(name = "hall_layouts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalonDuzeni {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hall_id", nullable = false, updatable = false)
    private Long salonId;

    @Column(name = "version")
    private Integer surum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DuzenDurumu durum;

    @Column(name = "grid_rows", nullable = false)
    private int satirSayisi;

    @Column(name = "grid_cols", nullable = false)
    private int sutunSayisi;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant guncellenmeZamani;

    @Column(name = "published_at")
    private Instant yayimlanmaZamani;

    private SalonDuzeni(Long salonId, int satirSayisi, int sutunSayisi) {
        this.salonId = salonId;
        this.durum = DuzenDurumu.DRAFT;
        this.satirSayisi = satirSayisi;
        this.sutunSayisi = sutunSayisi;
    }

    public static SalonDuzeni taslakOlustur(Long salonId, int satirSayisi, int sutunSayisi) {
        return new SalonDuzeni(salonId, satirSayisi, sutunSayisi);
    }

    public boolean taslakMi() {
        return durum == DuzenDurumu.DRAFT;
    }

    /** Yayımlanmış düzen değişmez; bu kuralı çağıranın unutmaması için entity de korur. */
    public void boyutuGuncelle(int satirSayisi, int sutunSayisi) {
        if (!taslakMi()) {
            throw new IllegalStateException("Yayımlanmış düzen değiştirilemez: " + id);
        }
        this.satirSayisi = satirSayisi;
        this.sutunSayisi = sutunSayisi;
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof SalonDuzeni duzen && id != null && id.equals(duzen.id);
    }

    @Override
    public int hashCode() {
        return SalonDuzeni.class.hashCode();
    }
}
