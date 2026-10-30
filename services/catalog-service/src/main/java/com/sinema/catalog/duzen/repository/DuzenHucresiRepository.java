package com.sinema.catalog.duzen.repository;

import com.sinema.catalog.duzen.entity.DuzenHucresi;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DuzenHucresiRepository extends JpaRepository<DuzenHucresi, Long> {

    List<DuzenHucresi> findAllByDuzenIdOrderBySatirAscSutunAsc(Long duzenId);

    /** Tek DELETE ifadesi; hemen çalışır, böylece ardından eklenen hücreler eski konumlarla çakışmaz. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DuzenHucresi h where h.duzenId = :duzenId")
    int duzeninHucreleriniSil(@Param("duzenId") Long duzenId);

    /** Sürüm listesindeki koltuk sayıları tek sorguda (her düzen için ayrı sorgu yok). */
    @Query("""
            select h.duzenId as duzenId, count(h) as koltukSayisi from DuzenHucresi h
            where h.duzenId in :duzenIdleri and h.tip = com.sinema.catalog.duzen.entity.HucreTipi.SEAT
            group by h.duzenId""")
    List<KoltukSayisi> koltukSayilari(@Param("duzenIdleri") Collection<Long> duzenIdleri);

    interface KoltukSayisi {
        Long getDuzenId();

        long getKoltukSayisi();
    }
}
