package com.sinema.catalog.film.dto;

import jakarta.validation.constraints.NotNull;

/** Film silinmez, gizlenir (K40). */
public record GorunurlukIstegi(@NotNull Boolean visible) {
}
