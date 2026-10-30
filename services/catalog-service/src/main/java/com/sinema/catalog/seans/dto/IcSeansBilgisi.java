package com.sinema.catalog.seans.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Booking'in koltuk tutarken ihtiyaç duyduğu seans bilgisi (planlama §3.3): satış kapanışı (K10), fiyat,
 * mühürlü düzen ve bilet snapshot'ı için adlar (§3.4-1). Yalnızca servis JWT'siyle (/internal) okunur.
 * Halka açık SeansDetayi'ndan ayrı tutulur: iki sözleşme birbirinden bağımsız değişebilsin.
 */
public record IcSeansBilgisi(
        Long showtimeId,
        Long movieId,
        String movieTitle,
        Long cinemaId,
        String cinemaName,
        Long hallId,
        String hallName,
        Long layoutId,
        Instant startsAt,
        Instant endsAt,
        Instant salesCloseAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal ticketPrice,
        Instant serverTime) {
}
