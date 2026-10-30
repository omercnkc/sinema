package com.sinema.catalog.sube.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Salon (halls). Tüm koltukları aynı fiyattır (K23); seans fiyatı buradan kopyalanır.
 * Aktif düzen sürümüne yalnızca id ile işaret eder; düzen yayımlanınca değişir (alt görev 3).
 */
@Entity
@Table(name = "halls")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Salon {

    /** DB kolonu DECIMAL(10,2): kuruş hassasiyeti. */
    public static final int FIYAT_ONDALIK_BASAMAGI = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false, updatable = false)
    private Sube sube;

    @Column(name = "name", nullable = false, length = 100)
    private String ad;

    @Column(name = "active_layout_id")
    private Long aktifDuzenId;

    @Column(name = "default_ticket_price", nullable = false, precision = 10, scale = FIYAT_ONDALIK_BASAMAGI)
    private BigDecimal varsayilanBiletFiyati;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant guncellenmeZamani;

    private Salon(Sube sube, String ad, BigDecimal varsayilanBiletFiyati) {
        this.sube = sube;
        bilgileriGuncelle(ad, varsayilanBiletFiyati);
    }

    public static Salon olustur(Sube sube, String ad, BigDecimal varsayilanBiletFiyati) {
        return new Salon(sube, ad, varsayilanBiletFiyati);
    }

    /**
     * Ölçek 2'ye sabitlenir: "120" ile "120.00" aynı değer olarak saklanır ve döner.
     * UNNECESSARY bilinçli: 2'den fazla basamak doğrulamada reddedilmiş olmalı, sessiz yuvarlama yapılmaz.
     */
    public void bilgileriGuncelle(String ad, BigDecimal varsayilanBiletFiyati) {
        this.ad = ad.trim();
        this.varsayilanBiletFiyati = varsayilanBiletFiyati.setScale(FIYAT_ONDALIK_BASAMAGI, RoundingMode.UNNECESSARY);
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof Salon salon && id != null && id.equals(salon.id);
    }

    @Override
    public int hashCode() {
        return Salon.class.hashCode();
    }
}
