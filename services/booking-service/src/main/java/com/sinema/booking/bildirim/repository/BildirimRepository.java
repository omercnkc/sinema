package com.sinema.booking.bildirim.repository;

import com.sinema.booking.bildirim.entity.Bildirim;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BildirimRepository extends JpaRepository<Bildirim, Long> {

    /** Okunmamışlar önce, sonra yeniden eskiye; (user_id, read_at, created_at) indeksiyle. */
    @Query(value = """
            select b from Bildirim b where b.kullaniciId = :kullaniciId
             order by case when b.okunmaZamani is null then 0 else 1 end, b.olusturulmaZamani desc, b.id desc""",
            countQuery = "select count(b) from Bildirim b where b.kullaniciId = :kullaniciId")
    Page<Bildirim> kullanicininBildirimleri(@Param("kullaniciId") Long kullaniciId, Pageable sayfa);

    long countByKullaniciIdAndOkunmaZamaniIsNull(Long kullaniciId);

    Optional<Bildirim> findByIdAndKullaniciId(Long id, Long kullaniciId);
}
