package com.sinema.payment.odeme.repository;

import com.sinema.payment.odeme.entity.Odeme;
import jakarta.persistence.LockModeType;
import java.util.Optional;
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
}
