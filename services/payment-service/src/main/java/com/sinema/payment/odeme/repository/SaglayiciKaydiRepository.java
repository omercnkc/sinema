package com.sinema.payment.odeme.repository;

import com.sinema.payment.odeme.entity.SaglayiciKaydi;
import org.springframework.data.jpa.repository.JpaRepository;

/** Ham sağlayıcı kayıtları; yalnızca eklenir (denetim), 90 gün sonra silinir (K43, alt görev 9). */
public interface SaglayiciKaydiRepository extends JpaRepository<SaglayiciKaydi, Long> {
}
