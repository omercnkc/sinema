package com.sinema.booking.doluluk.service;

import com.sinema.booking.config.RezervasyonAyarlari;
import com.sinema.booking.doluluk.KisaOmurluOnbellek;
import com.sinema.booking.doluluk.dto.DoluKoltuk;
import java.time.Clock;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Tek booking kopyası için bellek içi gerçekleştirme (K47): seans başına en çok
 * sinema.booking.doluluk-onbellek-suresi kadar; zamanlayıcı yok, eşzamanlı istekler tek sorguyu bekler
 * (ayrıntı KisaOmurluOnbellek'te).
 */
@Component
public class BellekDolulukOnbellegi implements DolulukOnbellegi {

    private final KisaOmurluOnbellek<Long, List<DoluKoltuk>> onbellek;

    public BellekDolulukOnbellegi(RezervasyonAyarlari ayarlar, Clock saat) {
        this.onbellek = new KisaOmurluOnbellek<>(ayarlar.dolulukOnbellekSuresi(), saat);
    }

    @Override
    public List<DoluKoltuk> getir(Long seansId, Supplier<List<DoluKoltuk>> yukleyici) {
        return onbellek.getir(seansId, yukleyici);
    }

    @Override
    public void sil(Long seansId) {
        onbellek.sil(seansId);
    }
}
