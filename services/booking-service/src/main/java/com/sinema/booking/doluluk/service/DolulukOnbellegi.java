package com.sinema.booking.doluluk.service;

import com.sinema.booking.doluluk.dto.DoluKoltuk;
import java.util.List;
import java.util.function.Supplier;

/**
 * Seans doluluğunun kısa ömürlü önbelleği (K47). Arayüz: booking birden fazla kopya çalıştırılırsa bellek yerine
 * paylaşılan bir önbellek (ör. Redis) yalnızca yeni bir gerçekleştirme sınıfıyla devreye girer, çağıranlar değişmez.
 * Doğruluk kaynağı her zaman MySQL'dir (K9); burası yalnızca polling yükünü azaltır.
 */
public interface DolulukOnbellegi {

    /** Taze kayıt varsa onu, yoksa yükleyicinin (DB sorgusu) sonucunu döner ve saklar. */
    List<DoluKoltuk> getir(Long seansId, Supplier<List<DoluKoltuk>> yukleyici);

    /** Seansın doluluğu değişti (hold, onay, bırakma): sonraki istek DB'den taze okur. */
    void sil(Long seansId);
}
