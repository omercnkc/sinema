package com.sinema.booking.goc.plan;

import java.util.List;

/** Bir seansın göç planı: her dolu koltuğun yeni yeri ve yerleştirilemeyen rezervasyonlar. */
public record SeansGocPlani(long seansId, List<KoltukTasima> tasimalar, List<CozulemeyenRezervasyon> cozulemeyenler) {

    public SeansGocPlani {
        tasimalar = List.copyOf(tasimalar);
        cozulemeyenler = List.copyOf(cozulemeyenler);
    }

    public boolean uygulanabilir() {
        return cozulemeyenler.isEmpty();
    }
}
