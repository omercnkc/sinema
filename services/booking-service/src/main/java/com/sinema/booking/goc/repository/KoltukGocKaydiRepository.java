package com.sinema.booking.goc.repository;

import com.sinema.booking.goc.entity.KoltukGocKaydi;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KoltukGocKaydiRepository extends JpaRepository<KoltukGocKaydi, Long> {

    List<KoltukGocKaydi> findAllByGocIdOrderById(Long gocId);

    /**
     * Rezervasyonun tamamlanmış göçlerdeki koltuk değişiklikleri (geri alınan ya da askıdaki göçün kayıtları
     * sayılmaz); etiketi de aynı kalan sessiz güncellemeler hariç.
     */
    @Query("""
            select k from KoltukGocKaydi k
             where k.rezervasyonId = :rezervasyonId
               and k.neden <> com.sinema.booking.goc.entity.GocNedeni.SAME_POSITION
               and k.gocId in (select g.id from DuzenGocu g
                                where g.durum = com.sinema.booking.goc.entity.GocDurumu.COMPLETED)
             order by k.olusturulmaZamani, k.id""")
    List<KoltukGocKaydi> rezervasyonunDegisiklikleri(@Param("rezervasyonId") Long rezervasyonId);
}
