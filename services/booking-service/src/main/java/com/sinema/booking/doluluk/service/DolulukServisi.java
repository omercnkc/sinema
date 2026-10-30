package com.sinema.booking.doluluk.service;

import com.sinema.booking.doluluk.dto.DolulukCevabi;
import com.sinema.booking.doluluk.dto.DoluKoltuk;
import com.sinema.booking.koltuk.repository.KoltukKaydiRepository;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Koltuk haritasının doluluk katmanı (planlama §3.4-2, §4.9): düzen catalog'dan bir kez gelir, bu uç polling ile
 * (5 sn, K33) yalnızca dolu koltukları verir.
 *
 * Seansın var olup olmadığı catalog'a sorulmaz: misafire açık ve sık çağrılan bir uç her istekte başka servise
 * gitmesin. Olmayan seans için cevap boş listedir; harita zaten catalog'dan seans bulunamadığı için açılmaz.
 */
@Service
@RequiredArgsConstructor
public class DolulukServisi {

    private final KoltukKaydiRepository koltukKaydiRepository;
    private final DolulukOnbellegi onbellek;
    private final Clock saat;

    public DolulukCevabi doluluk(Long seansId) {
        // Değişmez kopya: önbellekteki aynı liste tüm isteklere paylaştırılır
        List<DoluKoltuk> doluKoltuklar = onbellek.getir(seansId,
                () -> List.copyOf(koltukKaydiRepository.doluKoltuklar(seansId, saat.instant())));
        // Diğer cevaplarla aynı hassasiyet (DB DATETIME(3))
        return new DolulukCevabi(saat.instant().truncatedTo(ChronoUnit.MILLIS), doluKoltuklar);
    }
}
