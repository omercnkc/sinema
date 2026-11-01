package com.sinema.booking.goc;

import com.sinema.booking.katalog.istemci.KatalogGocKaynagi;
import com.sinema.booking.katalog.istemci.KatalogGocKaynagi.DuzenHucreleri;
import com.sinema.booking.katalog.istemci.KatalogGocKaynagi.Hucre;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Göç testleri için sahte catalog göç kaynağı. Eski düzen sabit: perde, 1. satır A-1..A-4 (901-904), 2. satır
 * B-1..B-4 (911-914). Yeni düzen metinle verilir (P perde, K koltuk, diğer boşluk).
 */
public final class GocTestKatalogu {

    public static final long ESKI_DUZEN = 900;
    public static final long YENI_DUZEN = 950;

    private GocTestKatalogu() {
    }

    /** İki başlamamış seans (ikisi de eski düzende): biletli ve biletsiz. */
    public static KatalogGocKaynagi kaynak(long salonId, long biletliSeans, long biletsizSeans, String... yeniSatirlar) {
        Instant yarin = Instant.now().plus(Duration.ofDays(1));
        return new KatalogGocKaynagi(salonId, duzen(YENI_DUZEN, yeniSatirlar),
                List.of(new KatalogGocKaynagi.Seans(biletliSeans, yarin, ESKI_DUZEN),
                        new KatalogGocKaynagi.Seans(biletsizSeans, yarin.plus(Duration.ofHours(3)), ESKI_DUZEN)),
                List.of(duzen(ESKI_DUZEN, "PPPP", "KKKK", "KKKK")));
    }

    /** Hücre kimliği = düzen + (satır - 1) * 10 + sütun + 1; etiketler perdeden itibaren A, B… soldan sağa. */
    public static long hucre(long duzenId, int satir, int sutun) {
        return duzenId + (satir - 1) * 10L + sutun + 1;
    }

    private static DuzenHucreleri duzen(long duzenId, String... satirlar) {
        List<Hucre> hucreler = new ArrayList<>();
        char sira = 'A';
        for (int satir = 0; satir < satirlar.length; satir++) {
            int numara = 1;
            for (int sutun = 0; sutun < satirlar[satir].length(); sutun++) {
                long id = hucre(duzenId, satir, sutun);
                switch (satirlar[satir].charAt(sutun)) {
                    case 'P' -> hucreler.add(new Hucre(id, satir, sutun, "SCREEN", null, null, null));
                    case 'K' -> hucreler.add(new Hucre(id, satir, sutun, "SEAT", String.valueOf(sira), numara++, "STANDARD"));
                    default -> { }
                }
            }
            if (satirlar[satir].indexOf('K') >= 0) {
                sira++;
            }
        }
        return new DuzenHucreleri(duzenId, satirlar.length, satirlar[0].length(), hucreler);
    }
}
