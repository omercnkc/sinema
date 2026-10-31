package com.sinema.payment.odeme.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Bir rezervasyonun ödemesi (payments). Rezervasyon başına tek kayıt (UNIQUE reservation_id): tekrar gelen "öde"
 * isteği yeni ödeme açmaz, aynısını döner (§7.2). Tutar booking'den alınır; istemciden gelmez (§4.8).
 * Durum geçişleri iş kuralı olarak bu sınıfın metotlarındadır; geçersiz geçiş IllegalStateException fırlatır.
 */
@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Odeme {

    public static final String PARA_BIRIMI = "TRY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false, updatable = false)
    private Long rezervasyonId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long kullaniciId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20, updatable = false)
    private OdemeSaglayicisi saglayici;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OdemeDurumu durum;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal tutar;

    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "CHAR(3)", updatable = false)
    private String paraBirimi;

    @Column(name = "conversation_id", nullable = false, length = 64, updatable = false)
    private String konusmaKimligi;

    @Column(name = "provider_token")
    private String saglayiciTokeni;

    @Column(name = "provider_token_expires_at")
    private Instant tokenBitisi;

    @Column(name = "payment_page_url", length = 500)
    private String odemeSayfasi;

    @Column(name = "provider_payment_id", length = 64)
    private String saglayiciOdemeKimligi;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_sync_status", nullable = false, length = 20)
    private BookingSenkronDurumu bookingSenkronDurumu;

    @Column(name = "booking_sync_attempts", nullable = false)
    private int bookingSenkronDenemesi;

    @Column(name = "next_retry_at")
    private Instant sonrakiDeneme;

    @Column(name = "failure_code", length = 50)
    private String hataKodu;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant guncellenmeZamani;

    /** Yeni ödeme kaydı (INITIATED): sağlayıcıda henüz başlatılmadı, iletilecek sonuç yok. */
    public static Odeme olustur(Long rezervasyonId, Long kullaniciId, OdemeSaglayicisi saglayici, BigDecimal tutar,
            String konusmaKimligi) {
        Odeme odeme = new Odeme();
        odeme.rezervasyonId = rezervasyonId;
        odeme.kullaniciId = kullaniciId;
        odeme.saglayici = saglayici;
        odeme.tutar = tutar;
        odeme.paraBirimi = PARA_BIRIMI;
        odeme.konusmaKimligi = konusmaKimligi;
        odeme.durum = OdemeDurumu.INITIATED;
        odeme.bookingSenkronDurumu = BookingSenkronDurumu.NOT_NEEDED;
        return odeme;
    }

    /** Sağlayıcıda ödeme sayfası açıldı: kullanıcı ödeme yapana kadar PENDING. Yalnızca INITIATED'dan. */
    public void saglayicidaBaslatildi(String token, Instant tokenBitisi, String odemeSayfasi) {
        if (durum != OdemeDurumu.INITIATED) {
            throw new IllegalStateException("Ödeme yalnızca INITIATED durumundayken başlatılabilir: " + durum);
        }
        this.saglayiciTokeni = token;
        this.tokenBitisi = tokenBitisi;
        this.odemeSayfasi = odemeSayfasi;
        this.durum = OdemeDurumu.PENDING;
    }

    /** Sağlayıcı ödemeyi onayladı ve kendi kaydımızla eşleşti: booking'e onay iletilecek (§4.5). */
    public void basariliOldu(String saglayiciOdemeKimligi) {
        sonucBekleniyorOlmali();
        this.saglayiciOdemeKimligi = saglayiciOdemeKimligi;
        this.durum = OdemeDurumu.SUCCEEDED;
        this.bookingSenkronDurumu = BookingSenkronDurumu.PENDING;
    }

    /** Ödeme reddedildi: booking'e ret iletilecek, koltuklar boşalacak. */
    public void basarisizOldu(String hataKodu) {
        sonucBekleniyorOlmali();
        this.durum = OdemeDurumu.FAILED;
        this.hataKodu = hataKodu;
        this.bookingSenkronDurumu = BookingSenkronDurumu.PENDING;
    }

    /**
     * Para alındı ama bilet verilemez (§4.8): sağlayıcı sonucu kendi kaydımızla eşleşmedi ya da booking onayı reddetti
     * (süre doldu). İade mutabakat job'ında yapılır; booking'e artık iletilecek bir şey yoktur.
     */
    public void iadeGerekli(String hataKodu) {
        if (durum != OdemeDurumu.PENDING && durum != OdemeDurumu.SUCCEEDED) {
            throw new IllegalStateException("İade yalnızca bekleyen ya da başarılı ödemede gerekebilir: " + durum);
        }
        this.durum = OdemeDurumu.REFUND_REQUIRED;
        this.hataKodu = hataKodu;
        this.bookingSenkronDurumu = BookingSenkronDurumu.FAILED;
    }

    /** İade bu kimlikle yapılır; bir kez yazılır (sonradan gelen bir değer mevcut kimliği ezmez). */
    public void saglayiciOdemeKimliginiKaydet(String kimlik) {
        if (this.saglayiciOdemeKimligi == null) {
            this.saglayiciOdemeKimligi = kimlik;
        }
    }

    /** Sonuç booking'e iletildi (onay ya da ret). */
    public void bookingSenkronlandi() {
        this.bookingSenkronDurumu = BookingSenkronDurumu.DONE;
        this.sonrakiDeneme = null;
    }

    /** booking'e ulaşılamadı: mutabakat job'ı sonrakiDeneme anında yeniden dener. */
    public void bookingSenkronuErtelendi(Instant sonrakiDeneme) {
        this.bookingSenkronDenemesi++;
        this.sonrakiDeneme = sonrakiDeneme;
    }

    /** Sağlayıcı sonucu henüz uygulanmadı mı (kullanıcı ödeme sayfasında ya da sonuç gelmedi). */
    public boolean sonucBekleniyorMu() {
        return durum == OdemeDurumu.PENDING;
    }

    private void sonucBekleniyorOlmali() {
        if (!sonucBekleniyorMu()) {
            throw new IllegalStateException("Sonuç yalnızca bekleyen ödemeye uygulanabilir: " + durum);
        }
    }

    /** Ödeme sayfası hâlâ kullanılabilir mi (tekrar gelen "öde" isteği aynı sayfaya dönebilir). */
    public boolean sayfaGecerliMi(Instant simdi) {
        return durum == OdemeDurumu.PENDING && tokenBitisi != null && tokenBitisi.isAfter(simdi);
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof Odeme odeme && id != null && id.equals(odeme.id);
    }

    @Override
    public int hashCode() {
        return Odeme.class.hashCode();
    }
}
