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

    /** Uygulama başlar: kayıt RUNNING açılır (salon kilidiyle aynı transaction'da). */
    public static DuzenGocu basla(Long salonId, Long hedefDuzenId, Long baslatanKullaniciId, Instant simdi) {
        DuzenGocu gocu = new DuzenGocu();
        gocu.salonId = salonId;
        gocu.hedefDuzenId = hedefDuzenId;
        gocu.baslatanKullaniciId = baslatanKullaniciId;
        gocu.durum = GocDurumu.RUNNING;
        gocu.olusturulmaZamani = simdi;
        return gocu;
    }

    public void tamamla(Instant simdi) {
        durum = GocDurumu.COMPLETED;
        tamamlanmaZamani = simdi;
    }

    /** Hiçbir şey kalıcı olarak değişmedi (yazılmadı ya da telafi edildi). */
    public void geriAlindi(String hataKodu, Instant simdi) {
        durum = GocDurumu.ROLLED_BACK;
        this.hataKodu = hataKodu;
        tamamlanmaZamani = simdi;
    }

    /** catalog'un durumu belirsiz (cevap alınamadı): biletler taşınmış kalır, salon kilitli; aynı göç sürdürülür. */
    public void askiyaAlindi(String hataKodu) {
        durum = GocDurumu.FAILED;
        this.hataKodu = hataKodu;
    }

    /** Aynı hedefle yeniden uygulandığında kaldığı yerden (catalog activate) sürdürülebilir mi. */
    public boolean surdurulebilir(Long hedef) {
        return durum == GocDurumu.FAILED && hedefDuzenId.equals(hedef);
    }

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
