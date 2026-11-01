package com.sinema.booking.goc.service;

import com.sinema.booking.goc.plan.SeansGocPlani;
import com.sinema.booking.katalog.istemci.KatalogGocKaynagi;
import java.util.List;

/**
 * Bir göçün hesaplanmış hâli: catalog'dan okunan kaynak ve bileti olan her başlamamış seansın planı. Önizleme bunu
 * rapora çevirir; uygulama aynı hesabı (kilit altında yeniden) uygular.
 */
public record GocHesabi(KatalogGocKaynagi kaynak, List<SeansGocPlani> planlar) {

    public GocHesabi {
        planlar = List.copyOf(planlar);
    }

    public boolean uygulanabilir() {
        return planlar.stream().allMatch(SeansGocPlani::uygulanabilir);
    }
}
