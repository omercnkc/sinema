package com.sinema.payment.odeme.repository;

import com.sinema.payment.odeme.entity.Odeme;
import com.sinema.payment.odeme.entity.OdemeSaglayicisi;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OdemeRepository extends JpaRepository<Odeme, Long> {

    /** Rezervasyon başına tek ödeme (UNIQUE reservation_id). */
    Optional<Odeme> findByRezervasyonId(Long rezervasyonId);

    /** Callback'teki token ödemeyi bulur (UNIQUE provider_token). */
    Optional<Odeme> findBySaglayiciTokeni(String saglayiciTokeni);

    /**
     * Sonuç uygulanırken ödeme satırı kilitlenir: aynı anda gelen iki callback (ya da callback ile mutabakat job'ı)
     * sırayla karar verir, sonuç bir kez uygulanır ve booking'e bir kez iletilir.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Odeme o where o.id = :id")
    Optional<Odeme> findKilitliById(@Param("id") Long id);

    /**
     * Mutabakat: booking'e iletilmesi gereken sonuçlar. Zamanı gelmiş yeniden denemeler ya da (hiç denenmemişse)
     * bir süredir dokunulmamış kayıtlar; böylece callback'in hâlâ işlediği bir ödeme ikinci kez alınmaz.
     */
    @Query("""
            select o.id from Odeme o
             where o.bookingSenkronDurumu = com.sinema.payment.odeme.entity.BookingSenkronDurumu.PENDING
               and ((o.sonrakiDeneme is not null and o.sonrakiDeneme <= :simdi)
                 or (o.sonrakiDeneme is null and o.guncellenmeZamani <= :sakinlikEsigi))
             order by o.id""")
    List<Long> senkronuBekleyenler(@Param("simdi") Instant simdi, @Param("sakinlikEsigi") Instant sakinlikEsigi,
            Limit limit);

    /**
     * Mutabakat: sonucu gelmemiş (callback gelmedi) ve bir süredir bekleyen ödemeler. Yalnızca verilen sağlayıcının
     * ödemeleri: sağlayıcı değiştirilirse (ör. MOCK → IYZICO) eski ödemeler yanlış sağlayıcıya sorulmaz.
     */
    @Query("""
            select o.id from Odeme o
             where o.durum = com.sinema.payment.odeme.entity.OdemeDurumu.PENDING
               and o.saglayici = :saglayici
               and o.guncellenmeZamani <= :sakinlikEsigi
             order by o.id""")
    List<Long> sonucuBeklenenler(@Param("saglayici") OdemeSaglayicisi saglayici,
            @Param("sakinlikEsigi") Instant sakinlikEsigi, Limit limit);

    /** Mutabakat: verilen sağlayıcının iadesi yapılacak ödemeleri (iade kimliği yoksa iade edilemez, elle incelenir). */
    @Query("""
            select o.id from Odeme o
             where o.durum = com.sinema.payment.odeme.entity.OdemeDurumu.REFUND_REQUIRED
               and o.saglayici = :saglayici
               and o.saglayiciOdemeKimligi is not null
               and (o.sonrakiDeneme is null or o.sonrakiDeneme <= :simdi)
             order by o.id""")
    List<Long> iadeyiBekleyenler(@Param("saglayici") OdemeSaglayicisi saglayici, @Param("simdi") Instant simdi,
            Limit limit);
}
