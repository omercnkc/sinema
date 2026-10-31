package com.sinema.payment.odeme.repository;

import com.sinema.payment.odeme.entity.Odeme;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OdemeRepository extends JpaRepository<Odeme, Long> {

    /** Rezervasyon başına tek ödeme (UNIQUE reservation_id). */
    Optional<Odeme> findByRezervasyonId(Long rezervasyonId);
}
