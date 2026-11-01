package com.sinema.booking.bildirim.entity;

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

/** Kullanıcıya uygulama içi bildirim (notifications; K35: e-posta yok). Kullanıcı auth'a yumuşak referanstır. */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bildirim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long kullaniciId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 30)
    private BildirimTuru tur;

    @Column(name = "reservation_id", updatable = false)
    private Long rezervasyonId;

    @Column(name = "message", nullable = false, updatable = false, length = 500)
    private String mesaj;

    @Column(name = "read_at")
    private Instant okunmaZamani;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    public static Bildirim koltukDegisti(Long kullaniciId, Long rezervasyonId, String mesaj, Instant simdi) {
        Bildirim bildirim = new Bildirim();
        bildirim.kullaniciId = kullaniciId;
        bildirim.tur = BildirimTuru.SEAT_CHANGED;
        bildirim.rezervasyonId = rezervasyonId;
        bildirim.mesaj = mesaj;
        bildirim.olusturulmaZamani = simdi;
        return bildirim;
    }

    /** Tekrar çağrı ilk okunma anını değiştirmez. */
    public void okundu(Instant simdi) {
        if (okunmaZamani == null) {
            okunmaZamani = simdi;
        }
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof Bildirim bildirim && id != null && id.equals(bildirim.id);
    }

    @Override
    public int hashCode() {
        return Bildirim.class.hashCode();
    }
}
