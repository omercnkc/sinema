package com.sinema.payment.odeme.repository;

import com.sinema.payment.odeme.entity.SaglayiciKaydi;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Ham sağlayıcı kayıtları; yalnızca eklenir (denetim), saklama süresi sonunda silinir (K43). */
public interface SaglayiciKaydiRepository extends JpaRepository<SaglayiciKaydi, Long> {

    /** En çok {@code parti} eski kaydı siler; tek büyük DELETE uzun süre kilit tutmasın. */
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM payment_callback_logs WHERE received_at < :esik LIMIT :parti", nativeQuery = true)
    int eskileriSil(@Param("esik") Instant esik, @Param("parti") int parti);
}
