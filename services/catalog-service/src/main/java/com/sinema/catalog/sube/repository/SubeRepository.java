package com.sinema.catalog.sube.repository;

import com.sinema.catalog.sube.entity.Sube;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubeRepository extends JpaRepository<Sube, Long> {

    /** Şubeler salonlarıyla tek sorguda gelir (N+1 yok). Şube sayısı küçük ve sabit olduğu için sayfalanmaz. */
    @EntityGraph(attributePaths = "salonlar")
    List<Sube> findAllByOrderByIdAsc();
}
