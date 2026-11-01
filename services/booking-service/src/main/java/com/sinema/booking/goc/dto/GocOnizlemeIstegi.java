package com.sinema.booking.goc.dto;

import jakarta.validation.constraints.NotNull;

/** Göç önizleme/uygulama: salon ve yayımlanacak taslak (catalog'da hazırlanmış). */
public record GocOnizlemeIstegi(@NotNull Long hallId, @NotNull Long toLayoutId) {
}
