package com.sinema.catalog.film.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Sayfalı liste cevabı (planlama §7.2). Spring'in Page nesnesi doğrudan dönülmez: JSON biçimi
 * sürümden sürüme değişebilir, sözleşme bizim elimizde kalsın. İkinci kullanıcı (booking) gelince
 * sinema-common'a taşınır.
 */
public record SayfaCevabi<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> SayfaCevabi<T> from(Page<T> sayfa) {
        return new SayfaCevabi<>(sayfa.getContent(), sayfa.getNumber(), sayfa.getSize(), sayfa.getTotalElements(),
                sayfa.getTotalPages());
    }
}
