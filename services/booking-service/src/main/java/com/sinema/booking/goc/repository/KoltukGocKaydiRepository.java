package com.sinema.booking.goc.repository;

import com.sinema.booking.goc.entity.KoltukGocKaydi;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KoltukGocKaydiRepository extends JpaRepository<KoltukGocKaydi, Long> {

    List<KoltukGocKaydi> findAllByGocIdOrderById(Long gocId);
}
