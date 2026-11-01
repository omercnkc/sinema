package com.sinema.catalog.duzen.dto;

/** Yayımlanan sürüm ve yeni sürüme geçen başlamamış seans sayısı. */
public record DuzenEtkinlestirmeCevabi(Long layoutId, int version, int migratedShowtimeCount) {
}
