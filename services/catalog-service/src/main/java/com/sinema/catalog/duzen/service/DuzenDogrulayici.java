package com.sinema.catalog.duzen.service;

import com.sinema.catalog.duzen.dto.DuzenIstegi;
import com.sinema.catalog.duzen.dto.HucreIstegi;
import com.sinema.catalog.duzen.entity.HucreTipi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Düzenin bütününe dair kurallar (alan başına Bean Validation'ın ifade edemediği kısım).
 * İlk hatada durmaz: admin editörde tüm sorunları bir kerede görsün diye ihlallerin listesini döner.
 * Durumsuz ve DB'ye bakmaz; birim testle tek başına sınanır.
 */
@Component
public class DuzenDogrulayici {

    public List<String> ihlalleriBul(DuzenIstegi istek) {
        List<String> ihlaller = new ArrayList<>();
        Set<String> konumlar = new HashSet<>();
        Set<String> koltukEtiketleri = new HashSet<>();
        boolean koltukVar = false;
        boolean perdeVar = false;

        for (HucreIstegi hucre : istek.cells()) {
            String konum = "(%d,%d)".formatted(hucre.row(), hucre.col());
            if (hucre.row() >= istek.gridRows() || hucre.col() >= istek.gridCols()) {
                ihlaller.add(konum + ": hücre ızgaranın dışında");
            }
            if (!konumlar.add(konum)) {
                ihlaller.add(konum + ": aynı konumda birden fazla hücre var");
            }
            if (hucre.type() == HucreTipi.SEAT) {
                koltukVar = true;
                koltukKurallariniUygula(hucre, konum, koltukEtiketleri, ihlaller);
            } else {
                perdeVar |= hucre.type() == HucreTipi.SCREEN;
                koltukDisiKurallariUygula(hucre, konum, ihlaller);
            }
        }
        if (!koltukVar) {
            ihlaller.add("Düzende en az bir koltuk olmalı");
        }
        if (!perdeVar) {
            ihlaller.add("Düzende perde (SCREEN) olmalı");
        }
        return ihlaller;
    }

    private static void koltukKurallariniUygula(HucreIstegi hucre, String konum, Set<String> koltukEtiketleri,
            List<String> ihlaller) {
        if (hucre.seatRowLabel() == null || hucre.seatNumber() == null || hucre.seatType() == null) {
            ihlaller.add(konum + ": koltukta sıra etiketi, numara ve koltuk tipi zorunlu");
            return;
        }
        String etiket = hucre.seatRowLabel() + "-" + hucre.seatNumber();
        if (!koltukEtiketleri.add(etiket)) {
            ihlaller.add(konum + ": " + etiket + " etiketi başka bir koltukta da var");
        }
    }

    private static void koltukDisiKurallariUygula(HucreIstegi hucre, String konum, List<String> ihlaller) {
        if (hucre.seatRowLabel() != null || hucre.seatNumber() != null || hucre.seatType() != null) {
            ihlaller.add(konum + ": koltuk olmayan hücrede koltuk bilgisi olamaz");
        }
    }
}
