package com.sinema.booking.koltuk.repository;

import com.sinema.booking.koltuk.entity.KoltukKaydi;
import org.springframework.data.jpa.repository.JpaRepository;

/** seat_bookings okuma (doluluk, alt görev 4). Yazma koşullu SQL ile KoltukKilitDeposu'nda (K9). */
public interface KoltukKaydiRepository extends JpaRepository<KoltukKaydi, Long> {
}
