package com.sinema.catalog.duzen.repository;

import com.sinema.catalog.duzen.entity.DuzenDurumu;
import com.sinema.catalog.duzen.entity.SalonDuzeni;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalonDuzeniRepository extends JpaRepository<SalonDuzeni, Long> {

    /** Düzen başka salona aitse bulunamamış sayılır (URL'deki salonla tutarlılık). */
    Optional<SalonDuzeni> findByIdAndSalonId(Long id, Long salonId);

    Optional<SalonDuzeni> findBySalonIdAndDurum(Long salonId, DuzenDurumu durum);

    /** En yeni üstte: önce taslak (id büyük), sonra yayımlanmış sürümler. */
    List<SalonDuzeni> findAllBySalonIdOrderByIdDesc(Long salonId);
}
