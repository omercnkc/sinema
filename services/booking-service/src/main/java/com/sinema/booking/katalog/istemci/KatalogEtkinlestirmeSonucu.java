package com.sinema.booking.katalog.istemci;

/** Yayımlanan sürüm ve yeni sürüme geçen başlamamış seans sayısı. */
public record KatalogEtkinlestirmeSonucu(Long layoutId, int version, int migratedShowtimeCount) {
}
