package com.sinema.booking.koltuk.repository;

import com.sinema.booking.doluluk.dto.DoluKoltuk;
import com.sinema.booking.koltuk.entity.KoltukKaydi;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** seat_bookings okuma. Yazma koşullu SQL ile KoltukKilitDeposu'nda (K9). */
public interface KoltukKaydiRepository extends JpaRepository<KoltukKaydi, Long> {

    /**
     * Seansın şu an dolu koltukları: satılmış ya da süresi dolmamış tutma. Süresi dolmuş HELD satır job'ı beklemeden
     * boş sayılır (§4.6). Entity yerine doğrudan DTO: yalnızca iki kolon okunur, (showtime_id, status,
     * hold_expires_at) indeksiyle karşılanır.
     */
    @Query("""
            select new com.sinema.booking.doluluk.dto.DoluKoltuk(k.hucreId, k.durum)
              from KoltukKaydi k
             where k.seansId = :seansId
               and (k.durum = com.sinema.booking.koltuk.entity.KoltukDurumu.SOLD or k.tutmaBitisi > :simdi)
             order by k.hucreId""")
    List<DoluKoltuk> doluKoltuklar(@Param("seansId") Long seansId, @Param("simdi") Instant simdi);
}
