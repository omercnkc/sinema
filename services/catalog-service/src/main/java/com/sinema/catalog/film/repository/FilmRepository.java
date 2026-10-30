package com.sinema.catalog.film.repository;

import com.sinema.catalog.film.entity.Film;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FilmRepository extends JpaRepository<Film, Long> {

    Optional<Film> findByTmdbId(Long tmdbId);

    /**
     * Sayfalı film araması. Boş (null) filtre uygulanmaz. Başlık karşılaştırması büyük/küçük harf ve
     * aksan duyarsızdır (kolon harmanlaması utf8mb4_0900_ai_ci): "dovus" araması "Dövüş Kulübü"nü bulur.
     */
    @Query("""
            select f from Film f
            where (:sadeceGorunur = false or f.gorunur = true)
              and (:arama is null or f.baslik like concat('%', :arama, '%'))
              and (:tur is null or :tur member of f.turler)""")
    Page<Film> ara(@Param("sadeceGorunur") boolean sadeceGorunur, @Param("arama") String arama,
            @Param("tur") String tur, Pageable sayfa);
}
