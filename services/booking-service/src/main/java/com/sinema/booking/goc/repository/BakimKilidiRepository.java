package com.sinema.booking.goc.repository;

import com.sinema.booking.goc.entity.BakimKilidi;
import org.springframework.data.jpa.repository.JpaRepository;

/** Kilit okuma. Kilit alma/kaldırma JDBC ile (KoltukGocDeposu): elle atanan kimlikte save() merge yapardı. */
public interface BakimKilidiRepository extends JpaRepository<BakimKilidi, Long> {
}
