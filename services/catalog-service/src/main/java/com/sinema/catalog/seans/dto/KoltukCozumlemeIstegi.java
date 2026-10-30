package com.sinema.catalog.seans.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Booking'in "bu hücreler bu seansın koltukları mı?" sorusu. Üst sınır cömert: iş kuralı (≤5, K24) booking'de. */
public record KoltukCozumlemeIstegi(@NotEmpty @Size(max = 50) List<@NotNull Long> cellIds) {
}
