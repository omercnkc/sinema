package com.sinema.booking.goc.plan;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Bir düzen sürümünün hücreleri (seansın eski sürümü ya da göçün hedef taslağı). */
public record GocDuzeni(long duzenId, List<GocHucresi> hucreler) {

    public GocDuzeni {
        hucreler = List.copyOf(hucreler);
    }

    List<GocHucresi> koltuklar() {
        return hucreler.stream().filter(GocHucresi::koltuk).toList();
    }

    Map<Long, GocHucresi> hucreIndeksi() {
        return hucreler.stream().collect(Collectors.toMap(GocHucresi::hucreId, Function.identity()));
    }

    /** Perde hücrelerinin ortalama sütunu; perde yoksa ızgara ortası sayılmaz, koltukların ortası alınır. */
    double perdeMerkezi() {
        List<GocHucresi> referans = hucreler.stream().filter(GocHucresi::perde).toList();
        List<GocHucresi> kaynak = referans.isEmpty() ? koltuklar() : referans;
        return kaynak.stream().mapToInt(GocHucresi::sutun).average().orElse(0);
    }
}
