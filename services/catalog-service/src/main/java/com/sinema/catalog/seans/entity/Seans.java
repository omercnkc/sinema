package com.sinema.catalog.seans.entity;

import com.sinema.catalog.film.entity.Film;
import com.sinema.catalog.sube.entity.Salon;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Seans (showtimes): film + salon + zaman (K1). Oluşturulduğu andaki salon düzeni sürümüne mühürlenir (K17);
 * düzen sonradan yayımlansa bile bu seansın koltukları değişmez.
 */
@Entity
@Table(name = "showtimes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seans {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false, updatable = false)
    private Film film;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_id", nullable = false, updatable = false)
    private Salon salon;

    @Column(name = "layout_id", nullable = false)
    private Long duzenId;

    @Column(name = "starts_at", nullable = false, updatable = false)
    private Instant baslangic;

    @Column(name = "ends_at", nullable = false, updatable = false)
    private Instant bitis;

    @Column(name = "ticket_price", nullable = false, precision = 10, scale = Salon.FIYAT_ONDALIK_BASAMAGI)
    private BigDecimal biletFiyati;

    @Column(name = "sales_close_offset_minutes", nullable = false)
    private int satisKapanisOfsetiDakika;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeansDurumu durum;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant guncellenmeZamani;

    /**
     * Bitiş, filmin süresi ve seans arası payından hesaplanır (K26); çağıran elle veremez.
     * Düzen, salonun o anki aktif sürümüdür (çağıran var olduğunu kontrol etmiş olmalı).
     */
    public static Seans olustur(Film film, Salon salon, Instant baslangic, Duration seansArasiPayi,
            BigDecimal biletFiyati, int satisKapanisOfsetiDakika) {
        Seans seans = new Seans();
        seans.film = film;
        seans.salon = salon;
        seans.duzenId = salon.getAktifDuzenId();
        seans.baslangic = baslangic;
        seans.bitis = baslangic.plus(Duration.ofMinutes(film.getSureDakika())).plus(seansArasiPayi);
        seans.durum = SeansDurumu.SCHEDULED;
        seans.satisBilgileriniGuncelle(biletFiyati, satisKapanisOfsetiDakika);
        return seans;
    }

    /** Fiyat değişikliği yalnızca bundan sonraki satışları etkiler; satılmış biletin fiyatı booking'de kopyalıdır. */
    public void satisBilgileriniGuncelle(BigDecimal biletFiyati, int satisKapanisOfsetiDakika) {
        this.biletFiyati = biletFiyati.setScale(Salon.FIYAT_ONDALIK_BASAMAGI, RoundingMode.UNNECESSARY);
        this.satisKapanisOfsetiDakika = satisKapanisOfsetiDakika;
    }

    /** K10: satış, başlangıç + ofset anına kadar (o an dahil) açıktır (§2.4-3). */
    public Instant satisKapanisi() {
        return baslangic.plus(Duration.ofMinutes(satisKapanisOfsetiDakika));
    }

    public boolean satisAcikMi(Instant simdi) {
        return !simdi.isAfter(satisKapanisi());
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof Seans seans && id != null && id.equals(seans.id);
    }

    @Override
    public int hashCode() {
        return Seans.class.hashCode();
    }
}
