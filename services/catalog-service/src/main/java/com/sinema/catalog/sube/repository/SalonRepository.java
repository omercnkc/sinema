package com.sinema.catalog.sube.repository;

import com.sinema.catalog.sube.entity.Salon;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    boolean existsBySubeIdAndAd(Long subeId, String ad);

    boolean existsBySubeIdAndAdAndIdNot(Long subeId, String ad, Long id);

    /**
     * Salon satırını transaction sonuna kadar kilitler (SELECT ... FOR UPDATE). Aynı salonda düzen taslağı
     * açma, yayımlama ve seans ekleme bu kilitle sıraya girer (db-planlama §3.2).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Salon s where s.id = :id")
    Optional<Salon> findKilitliById(@Param("id") Long id);
}
