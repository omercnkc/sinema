package com.sinema.catalog.sube.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Sinema şubesi (cinemas). Silinmez; salonları üzerinden seanslara bağlıdır. */
@Entity
@Table(name = "cinemas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA ister; uygulama kodu fabrika metodunu kullanır
public class Sube {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String ad;

    @Column(name = "city", nullable = false, length = 100)
    private String sehir;

    @Column(name = "address", nullable = false)
    private String adres;

    // Yalnızca okuma için (şube listesi salonlarıyla döner); salon kendi tarafından kaydedilir
    @OneToMany(mappedBy = "sube")
    @OrderBy("id")
    private List<Salon> salonlar = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant guncellenmeZamani;

    private Sube(String ad, String sehir, String adres) {
        bilgileriGuncelle(ad, sehir, adres);
    }

    public static Sube olustur(String ad, String sehir, String adres) {
        return new Sube(ad, sehir, adres);
    }

    public void bilgileriGuncelle(String ad, String sehir, String adres) {
        this.ad = ad.trim();
        this.sehir = sehir.trim();
        this.adres = adres.trim();
    }

    public List<Salon> getSalonlar() {
        return Collections.unmodifiableList(salonlar);
    }

    // Kimlik id'dir; hashCode sabittir çünkü id kayıtta atanır
    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof Sube sube && id != null && id.equals(sube.id);
    }

    @Override
    public int hashCode() {
        return Sube.class.hashCode();
    }
}
