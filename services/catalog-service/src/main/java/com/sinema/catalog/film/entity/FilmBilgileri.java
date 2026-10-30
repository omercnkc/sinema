package com.sinema.catalog.film.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Filmin admin tarafından düzenlenebilen alanları. Entity'nin API DTO'suna bağımlı olmaması için
 * oluşturma/güncelleme bu değer nesnesiyle yapılır (istek → FilmBilgileri dönüşümü mapper'da).
 */
public record FilmBilgileri(
        String baslik,
        String ozet,
        String afisYolu,
        int sureDakika,
        LocalDate vizyonTarihi,
        String yasSiniri,
        BigDecimal tmdbPuani,
        String yonetmen,
        Set<String> turler,
        List<String> oyuncular,
        Set<AkilliIsaret> akilliIsaretler) {

    /**
     * TMDB yenilemesi (K45) için: yeni değeri dolu olan alan güncellenir, boş gelen alan mevcut değeri korur.
     * Böylece TMDB'de olmayan bilgi (ör. Türkçe özet, TR yaş sınırı, akıllı işaret) admin elle girdiyse kaybolmaz.
     * Statik bilinçli: "tek parametre alıp aynı tipi dönen" örnek metodu MapStruct fluent setter sanıyor.
     */
    public static FilmBilgileri bosOlmayanlarlaBirlestir(FilmBilgileri mevcut, FilmBilgileri yeni) {
        return new FilmBilgileri(
                ilkDolu(yeni.baslik, mevcut.baslik),
                ilkDolu(yeni.ozet, mevcut.ozet),
                ilkDolu(yeni.afisYolu, mevcut.afisYolu),
                yeni.sureDakika > 0 ? yeni.sureDakika : mevcut.sureDakika,
                ilkDolu(yeni.vizyonTarihi, mevcut.vizyonTarihi),
                ilkDolu(yeni.yasSiniri, mevcut.yasSiniri),
                ilkDolu(yeni.tmdbPuani, mevcut.tmdbPuani),
                ilkDolu(yeni.yonetmen, mevcut.yonetmen),
                ilkDoluListe(yeni.turler, mevcut.turler),
                ilkDoluListe(yeni.oyuncular, mevcut.oyuncular),
                ilkDoluListe(yeni.akilliIsaretler, mevcut.akilliIsaretler));
    }

    private static <T> T ilkDolu(T yeni, T mevcut) {
        return yeni != null ? yeni : mevcut;
    }

    private static <T extends Collection<?>> T ilkDoluListe(T yeni, T mevcut) {
        return yeni != null && !yeni.isEmpty() ? yeni : mevcut;
    }
}
