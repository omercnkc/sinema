package com.sinema.catalog.seans.repository;

import com.sinema.catalog.seans.entity.Seans;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeansRepository extends JpaRepository<Seans, Long> {

    /**
     * Salonda [baslangic, bitis) aralığıyla kesişen seans (§2.4-8). Aralıklar yarı açık: biri tam
     * diğerinin bittiği anda başlayabilir. Salon satırı kilitliyken çağrılır, böylece iki eşzamanlı
     * ekleme birbirini görmeden çakışamaz (tek UNIQUE kısıtla ifade edilemeyen kural, db-planlama §3.2).
     */
    @Query("""
            select s.id from Seans s
            where s.salon.id = :salonId and s.baslangic < :bitis and s.bitis > :baslangic
            order by s.baslangic""")
    List<Long> cakisanSeansIdleri(@Param("salonId") Long salonId, @Param("baslangic") Instant baslangic,
            @Param("bitis") Instant bitis, Limit limit);

    /** K13: salonda şu an süren seans var mı (düzen yayımlamayı engeller). */
    @Query("""
            select count(s) > 0 from Seans s
            where s.salon.id = :salonId and s.baslangic <= :an and s.bitis > :an""")
    boolean surenSeansVarMi(@Param("salonId") Long salonId, @Param("an") Instant an);

    /** Film detayındaki seans listesi; salon ve şube aynı sorguda gelir (N+1 yok). */
    @Query("""
            select s from Seans s join fetch s.salon salon join fetch salon.sube sube
            where s.film.id = :filmId and s.baslangic >= :gunBasi and s.baslangic < :gunSonu
              and (:subeId is null or sube.id = :subeId)
            order by s.baslangic, sube.id, salon.id""")
    List<Seans> filminGunlukSeanslari(@Param("filmId") Long filmId, @Param("gunBasi") Instant gunBasi,
            @Param("gunSonu") Instant gunSonu, @Param("subeId") Long subeId);

    /** Seans detayı: film, salon ve şube tek sorguda. */
    @Query("""
            select s from Seans s join fetch s.film join fetch s.salon salon join fetch salon.sube
            where s.id = :seansId""")
    Optional<Seans> detayIleBul(@Param("seansId") Long seansId);
}
