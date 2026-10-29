package com.sinema.auth.kullanici.repository;

import com.sinema.auth.kullanici.entity.Kullanici;
import com.sinema.auth.kullanici.entity.Rol;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface KullaniciRepository extends JpaRepository<Kullanici, Long> {

    boolean existsByEmail(String email);

    Optional<Kullanici> findByEmail(String email);

    boolean existsByRol(Rol rol);

    /**
     * Başarısız girişi TEK SQL ifadesiyle sayar; eşiğe ulaşınca hesabı kilitleyip sayacı sıfırlar.
     * "Oku → +1 → yaz" yapılsaydı aynı anda gelen hatalı denemeler birbirinin artışını ezer,
     * saldırgan paralel istekle kilidi atlatabilirdi. Tek UPDATE'te bu mümkün değil.
     * Sıra önemli: MySQL SET'i soldan sağa uygular ve sonraki atamalar güncellenmiş değeri görür;
     * bu yüzden locked_until, sayaç değişmeden ÖNCE hesaplanır.
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE users
            SET locked_until = CASE WHEN failed_login_count + 1 >= :esik THEN :kilitBitisi ELSE locked_until END,
                failed_login_count = CASE WHEN failed_login_count + 1 >= :esik THEN 0 ELSE failed_login_count + 1 END,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = :id
            """, nativeQuery = true)
    int basarisizGirisiKaydet(@Param("id") Long id, @Param("esik") int esik, @Param("kilitBitisi") Instant kilitBitisi);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE users
            SET failed_login_count = 0, locked_until = NULL, updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = :id
            """, nativeQuery = true)
    int girisSayaclariniSifirla(@Param("id") Long id);
}
