package com.sinema.booking.rezervasyon.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * POST /api/v1/bookings/reservations gövdesi (planlama §7.5). Koltuk sayısı üst sınırı (K24) yapılandırmadan
 * okunduğu için servis kontrol eder; burada yalnızca biçim.
 */
public record KoltukTutmaIstegi(
        @NotNull @Positive Long showtimeId,
        @NotEmpty List<@NotNull @Positive Long> seatCellIds) {
}
