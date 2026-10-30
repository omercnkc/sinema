package com.sinema.booking.rezervasyon.repository;

import com.sinema.booking.rezervasyon.entity.Rezervasyon;
import com.sinema.booking.rezervasyon.entity.RezervasyonDurumu;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RezervasyonRepository extends JpaRepository<Rezervasyon, Long> {

    /** Kullanıcının bu seanstaki ödeme bekleyen rezervasyonu (en çok bir tane, K24). Süresi dolmuş olabilir. */
    Optional<Rezervasyon> findByKullaniciIdAndSeansIdAndDurum(Long kullaniciId, Long seansId,
            RezervasyonDurumu durum);

    /**
     * Süresi dolmuş ama job henüz kapatmamış bekleyen rezervasyonu EXPIRED yapar. Yeni hold'dan hemen önce çağrılır:
     * aksi halde active_pending_key UNIQUE'i, süresi dolmuş eski işlem yüzünden yeni işlemi reddederdi.
     * Koltuk satırlarına dokunmaz: süresi dolmuş HELD satırlar zaten devralınabilir (§4.6).
     */
    @Modifying
    @Query("""
            update Rezervasyon r
               set r.durum = com.sinema.booking.rezervasyon.entity.RezervasyonDurumu.EXPIRED,
                   r.guncellenmeZamani = :simdi
             where r.kullaniciId = :kullaniciId and r.seansId = :seansId
               and r.durum = com.sinema.booking.rezervasyon.entity.RezervasyonDurumu.PENDING_PAYMENT
               and r.bitis <= :simdi""")
    int suresiDolanBekleyeniKapat(@Param("kullaniciId") Long kullaniciId, @Param("seansId") Long seansId,
            @Param("simdi") Instant simdi);

    /** Sahiplik sorgunun parçası: başkasının rezervasyonu "yok" gibi görünür (404, §7.2). */
    Optional<Rezervasyon> findByIdAndKullaniciId(Long id, Long kullaniciId);

    Page<Rezervasyon> findByKullaniciId(Long kullaniciId, Pageable sayfa);

    /**
     * Hold'u bırakır (K40), yalnızca hâlâ bırakılabilirse: sahibi, ödeme bekliyor, süresi dolmamış ve ödeme
     * başlatılmamış (ödeme penceresi uzatılmamış, K32). Tek koşullu UPDATE: eşzamanlı gelen ödeme onayıyla
     * (alt görev 8) yarışta yalnızca biri kazanır, ikisi birden uygulanamaz. Dönüş 1 ise bırakıldı.
     */
    @Modifying
    @Query("""
            update Rezervasyon r
               set r.durum = com.sinema.booking.rezervasyon.entity.RezervasyonDurumu.RELEASED,
                   r.birakilmaZamani = :simdi, r.guncellenmeZamani = :simdi
             where r.id = :id and r.kullaniciId = :kullaniciId
               and r.durum = com.sinema.booking.rezervasyon.entity.RezervasyonDurumu.PENDING_PAYMENT
               and r.odemePenceresiUzatildi = false and r.bitis > :simdi""")
    int birak(@Param("id") Long id, @Param("kullaniciId") Long kullaniciId, @Param("simdi") Instant simdi);

    /**
     * K29: kullanıcının [pencereBasi, simdi] aralığında terk ettiği işlemlerin terk anları. Terk = bıraktı (RELEASED,
     * an = bırakma) ya da ödemeden süresi doldu (EXPIRED veya job'ın henüz kapatmadığı süresi geçmiş
     * PENDING_PAYMENT, an = bitiş). Ödenen (CONFIRMED) ve reddedilen (FAILED) işlemler sayılmaz (§4.7).
     */
    @Query("""
            select case when r.durum = com.sinema.booking.rezervasyon.entity.RezervasyonDurumu.RELEASED
                        then r.birakilmaZamani else r.bitis end
              from Rezervasyon r
             where r.kullaniciId = :kullaniciId
               and ((r.durum = com.sinema.booking.rezervasyon.entity.RezervasyonDurumu.RELEASED
                     and r.birakilmaZamani > :pencereBasi)
                 or (r.durum in (com.sinema.booking.rezervasyon.entity.RezervasyonDurumu.EXPIRED,
                                 com.sinema.booking.rezervasyon.entity.RezervasyonDurumu.PENDING_PAYMENT)
                     and r.bitis > :pencereBasi and r.bitis <= :simdi))""")
    List<Instant> terkZamanlari(@Param("kullaniciId") Long kullaniciId, @Param("pencereBasi") Instant pencereBasi,
            @Param("simdi") Instant simdi);

    /**
     * Ödeme akışı (uzatma, onay, ret) rezervasyon satırını kilitleyerek okur: aynı rezervasyon üzerindeki işlemler
     * (ve koşullu UPDATE ile çalışan bırakma) sırayla uygulanır, ikisi aynı anda karar veremez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rezervasyon r where r.id = :id")
    Optional<Rezervasyon> findKilitliById(@Param("id") Long id);

    /**
     * Süre dolumu job'ı (§4.6): süresi geçmiş bekleyen işlemleri EXPIRED yapar, en çok {@code parti} satır.
     * Parti sınırı: tek büyük UPDATE uzun süre kilit tutmasın. Doğruluk buna bağlı değil (sorgular zamanı kontrol eder).
     */
    @Transactional
    @Modifying
    @Query(value = """
            UPDATE reservations SET status = 'EXPIRED', updated_at = :simdi
             WHERE status = 'PENDING_PAYMENT' AND expires_at <= :simdi
             LIMIT :parti""", nativeQuery = true)
    int suresiDolanlariKapat(@Param("simdi") Instant simdi, @Param("parti") int parti);
}
