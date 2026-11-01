package com.sinema.booking.goc.service;

import com.sinema.booking.goc.entity.KoltukGocKaydi;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Düzen göçü bildiriminin metni (K35). Sıra kaldırılınca alttaki sıra harfini devralabildiği için yedek atanan
 * koltuğun etiketi aynı kalabilir; o zaman "A-1 → A-1" yerine "A-1 (yeni yer)" yazılır: koltuk fiziksel olarak
 * değişti, müşteri biletteki etiketle doğru yere oturur.
 */
final class KoltukDegisikligiMesaji {

    private KoltukDegisikligiMesaji() {
    }

    static String olustur(String filmAdi, String subeAdi, String salonAdi, List<KoltukGocKaydi> degisiklikler) {
        String koltuklar = degisiklikler.stream()
                .sorted(Comparator.comparing(KoltukGocKaydi::getEskiEtiket))
                .map(KoltukDegisikligiMesaji::koltuk)
                .collect(Collectors.joining(", "));
        return "Salon düzeni değişti: %s (%s, %s) biletinizin koltuğu güncellendi: %s."
                .formatted(filmAdi, subeAdi, salonAdi, koltuklar);
    }

    private static String koltuk(KoltukGocKaydi kayit) {
        return kayit.getEskiEtiket().equals(kayit.getYeniEtiket())
                ? kayit.getYeniEtiket() + " (yeni yer)"
                : kayit.getEskiEtiket() + " → " + kayit.getYeniEtiket();
    }
}
