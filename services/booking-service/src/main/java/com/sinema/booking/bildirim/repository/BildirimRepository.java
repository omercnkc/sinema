package com.sinema.booking.bildirim.repository;

import com.sinema.booking.bildirim.entity.Bildirim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BildirimRepository extends JpaRepository<Bildirim, Long> {
}
