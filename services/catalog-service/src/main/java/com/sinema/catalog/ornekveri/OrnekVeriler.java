package com.sinema.catalog.ornekveri;

import com.sinema.catalog.film.dto.FilmIstegi;
import com.sinema.catalog.film.entity.AkilliIsaret;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Örnek veri (K2, K27): İstanbul'da 3 şube, 3/4/3 salon, her salonun kendine özgü düzeni.
 * Filmler ve kişiler uydurmadır; TMDB'den veri kopyalanmaz (TMDB içeriği repoda kalıcı önbellek olurdu, K45).
 */
final class OrnekVeriler {

    record OrnekSalon(String ad, String fiyat, DuzenSablonu duzen) {
    }

    record OrnekSube(String ad, String adres, List<OrnekSalon> salonlar) {
    }

    static final String SEHIR = "İstanbul";

    static final List<OrnekSube> SUBELER = List.of(
            new OrnekSube("Kadıköy", "Moda Caddesi No: 12, Kadıköy", List.of(
                    new OrnekSalon("Salon 1", "160", new DuzenSablonu(10, 14, List.of(7), true, 0)),
                    new OrnekSalon("Salon 2", "160", new DuzenSablonu(8, 12, List.of(4, 8), false, 1)),
                    new OrnekSalon("Salon 3", "140", new DuzenSablonu(6, 10, List.of(), true, 2)))),
            new OrnekSube("Beşiktaş", "Barbaros Bulvarı No: 45, Beşiktaş", List.of(
                    new OrnekSalon("Salon 1", "180", new DuzenSablonu(12, 16, List.of(4, 12), true, 1)),
                    new OrnekSalon("Salon 2", "170", new DuzenSablonu(9, 12, List.of(6), false, 0)),
                    new OrnekSalon("Salon 3", "150", new DuzenSablonu(7, 10, List.of(5), true, 2)),
                    new OrnekSalon("Salon 4", "150", new DuzenSablonu(5, 8, List.of(), false, 0)))),
            new OrnekSube("Bakırköy", "İncirli Caddesi No: 8, Bakırköy", List.of(
                    new OrnekSalon("Salon 1", "150", new DuzenSablonu(11, 18, List.of(5, 13), false, 2)),
                    new OrnekSalon("Salon 2", "140", new DuzenSablonu(8, 14, List.of(7), true, 1)),
                    new OrnekSalon("Salon 3", "130", new DuzenSablonu(6, 9, List.of(3), false, 0)))));

    static final List<FilmIstegi> FILMLER = List.of(
            film("Sisli Liman", "Bir liman kasabasında kaybolan teknenin izini süren dedektifin hikayesi.", 118,
                    "16+", "7.4", Set.of("Gerilim", "Gizem"), "Deniz Arslan",
                    List.of("Ece Yılmaz", "Can Demir", "Selin Kaya"), Set.of(AkilliIsaret.SIDDET_KORKU)),
            film("Mahalle Bakkalı", "Zincir marketlere karşı direnen bir bakkalın komik mücadelesi.", 96,
                    "Genel İzleyici Kitlesi", "6.8", Set.of("Komedi", "Aile"), "Ayşe Çınar",
                    List.of("Mert Aydın", "Zeynep Şahin"), Set.of()),
            film("Yıldız Haritası", "Kayıp bir uzay istasyonundan gelen sinyalin peşindeki mürettebat.", 142,
                    "13+", "8.1", Set.of("Bilim Kurgu", "Macera"), "Kerem Özkan",
                    List.of("Elif Koç", "Burak Yıldız", "Deniz Öztürk"), Set.of(AkilliIsaret.SIDDET_KORKU)),
            film("Kuyruklu Kuyruk", "Evden kaçan meraklı bir kedinin şehirde geçen günü.", 88,
                    "Genel İzleyici Kitlesi", "7.0", Set.of("Animasyon", "Aile"), "Okan Güler",
                    List.of("Seslendirme: Ali Polat", "Seslendirme: Naz Erdem"), Set.of()),
            film("Son Tren", "Gece yarısı treninde birbirini tanımayan yedi yolcunun sırrı.", 124,
                    "18+", "7.7", Set.of("Dram", "Gerilim"), "Leyla Aksoy",
                    List.of("Tolga Kurt", "Ceren Aslan", "Emre Uçar"),
                    Set.of(AkilliIsaret.SIDDET_KORKU, AkilliIsaret.OLUMSUZ_ORNEK)));

    private OrnekVeriler() {
    }

    private static FilmIstegi film(String baslik, String ozet, int sure, String yasSiniri, String puan,
            Set<String> turler, String yonetmen, List<String> oyuncular, Set<AkilliIsaret> isaretler) {
        return new FilmIstegi(null, baslik, ozet, null, sure, LocalDate.of(2026, 10, 1), yasSiniri,
                new BigDecimal(puan), yonetmen, turler, oyuncular, isaretler);
    }
}
