package com.sinema.booking.rezervasyon.repository;

import com.sinema.booking.rezervasyon.entity.Rezervasyon;
import com.sinema.booking.rezervasyon.entity.RezervasyonDurumu;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
