package com.sinema.auth.oturum.repository;

import com.sinema.auth.oturum.entity.YenilemeTokeni;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface YenilemeTokeniRepository extends JpaRepository<YenilemeTokeni, Long> {

    /**
     * SELECT ... FOR UPDATE: aynı token'la gelen iki yenileme isteği sıraya girer.
     * İkincisi, birincinin rotasyonunu (iptal + yerine geçen) görerek karar verir.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<YenilemeTokeni> findByTokenOzeti(String tokenOzeti);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update YenilemeTokeni t set t.iptalZamani = :simdi where t.aileKimligi = :aile and t.iptalZamani is null")
    int aileyiIptalEt(@Param("aile") String aileKimligi, @Param("simdi") Instant simdi);

    @Modifying
    @Query("delete from YenilemeTokeni t where t.sonGecerlilik < :simdi")
    int suresiDolanlariSil(@Param("simdi") Instant simdi);
}
