package com.sinema.booking.rezervasyon.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Bir satın alma işlemi (reservations): kullanıcı + seans + en çok 5 koltuk (K24). Bilet görüntü verisi
 * (film/şube/salon adı, seans saati) oluşturma anında catalog'dan kopyalanır (snapshot, §3.4-1); doğrulama sayfası
 * catalog'a bağımlı olmadan çalışır.
 *
 * "Seansta tek aktif rezervasyon" kuralı (K24) DB'deki üretilmiş active_pending_key kolonu + UNIQUE ile korunur;
 * kolon entity'de eşlenmez, MySQL kendisi hesaplar.
 */
@Entity
@Table(name = "reservations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rezervasyon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long kullaniciId;

    @Column(name = "showtime_id", nullable = false, updatable = false)
    private Long seansId;

    @Column(name = "hall_id", nullable = false, updatable = false)
    private Long salonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RezervasyonDurumu durum;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal toplamTutar;

    @Column(name = "expires_at", nullable = false)
    private Instant bitis;

    @Column(name = "payment_window_extended", nullable = false)
    private boolean odemePenceresiUzatildi;

    @Column(name = "released_at")
    private Instant birakilmaZamani;

    @Column(name = "ticket_code", length = 32)
    private String biletKodu;

    @Column(name = "movie_title", nullable = false, updatable = false)
    private String filmAdi;

    @Column(name = "cinema_name", nullable = false, updatable = false, length = 150)
    private String subeAdi;

    @Column(name = "hall_name", nullable = false, updatable = false, length = 100)
    private String salonAdi;

    @Column(name = "starts_at", nullable = false, updatable = false)
    private Instant seansBaslangici;

    /** Ödeme penceresi uzatması bu anı aşamaz (K10, K32). */
    @Column(name = "sales_close_at", nullable = false, updatable = false)
    private Instant satisKapanisi;

    // Listede her rezervasyonun koltukları tek tek değil toplu yüklenir (N+1 yok)
    @ElementCollection
    @BatchSize(size = 50)
    @CollectionTable(name = "reservation_seats", joinColumns = @JoinColumn(name = "reservation_id"))
    @OrderBy("hucreId")
    private List<RezervasyonKoltugu> koltuklar = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    @Column(name = "confirmed_at")
    private Instant onaylanmaZamani;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant guncellenmeZamani;

    /** Ödeme bekleyen yeni rezervasyon; toplam tutar koltuk fiyatlarından hesaplanır, çağıran veremez. */
    public static Rezervasyon beklemedeOlustur(Long kullaniciId, SeansKopyasi seans, List<RezervasyonKoltugu> koltuklar,
            Instant bitis) {
        Rezervasyon rezervasyon = new Rezervasyon();
        rezervasyon.kullaniciId = kullaniciId;
        rezervasyon.seansId = seans.seansId();
        rezervasyon.salonId = seans.salonId();
        rezervasyon.filmAdi = seans.filmAdi();
        rezervasyon.subeAdi = seans.subeAdi();
        rezervasyon.salonAdi = seans.salonAdi();
        rezervasyon.seansBaslangici = seans.baslangic();
        rezervasyon.satisKapanisi = seans.satisKapanisi();
        rezervasyon.durum = RezervasyonDurumu.PENDING_PAYMENT;
        rezervasyon.bitis = bitis;
        rezervasyon.koltuklar.addAll(koltuklar);
        rezervasyon.toplamTutar = koltuklar.stream()
                .map(RezervasyonKoltugu::getBirimFiyat)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return rezervasyon;
    }

    /** Ödeme hâlâ yapılabilir mi: ödeme bekliyor ve süresi dolmamış. */
    public boolean odenebilirMi(Instant simdi) {
        return durum == RezervasyonDurumu.PENDING_PAYMENT && bitis.isAfter(simdi);
    }

    /**
     * K32: ödeme başlatılınca hold bir kez uzar; yeni bitiş satış kapanışını aşamaz. İkinci çağrı bir şey yapmaz
     * (payment-service yeniden deneyebilir). Çağıran odenebilirMi'yi kontrol etmiş olmalı.
     */
    public void odemePenceresiniUzat(Duration pencere) {
        if (odemePenceresiUzatildi) {
            return;
        }
        Instant uzamis = bitis.plus(pencere);
        bitis = uzamis.isBefore(satisKapanisi) ? uzamis : satisKapanisi;
        odemePenceresiUzatildi = true;
    }

    /** Ödeme başarılı: bilet kodu yalnızca onaylı rezervasyonda olur (K30). */
    public void onayla(String biletKodu, Instant simdi) {
        this.biletKodu = biletKodu;
        this.durum = RezervasyonDurumu.CONFIRMED;
        this.onaylanmaZamani = simdi;
    }

    /** Ödeme reddedildi; koltukları çağıran serbest bırakır. */
    public void basarisizYap() {
        this.durum = RezervasyonDurumu.FAILED;
    }

    public List<RezervasyonKoltugu> getKoltuklar() {
        return Collections.unmodifiableList(koltuklar);
    }

    /** Bilet snapshot'ı için gereken seans bilgisi (catalog'dan, oluşturma anında). */
    public record SeansKopyasi(Long seansId, Long salonId, String filmAdi, String subeAdi, String salonAdi,
            Instant baslangic, Instant satisKapanisi) {
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof Rezervasyon rezervasyon && id != null && id.equals(rezervasyon.id);
    }

    @Override
    public int hashCode() {
        return Rezervasyon.class.hashCode();
    }
}
