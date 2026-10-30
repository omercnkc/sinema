package com.sinema.catalog.film.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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
}
