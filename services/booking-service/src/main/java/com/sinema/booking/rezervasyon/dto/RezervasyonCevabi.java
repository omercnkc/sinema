package com.sinema.booking.rezervasyon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sinema.booking.rezervasyon.entity.RezervasyonDurumu;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Rezervasyon cevabı (planlama §7.5). Geri sayım expiresAt ve serverTime farkından hesaplanır, istemci saatine
 * güvenilmez (§4.3). Para string ("150.00"), kayan nokta kaybı yok (§7.2).
 */
public record RezervasyonCevabi(
        Long reservationId,
        Long showtimeId,
        RezervasyonDurumu status,
        Instant expiresAt,
        Instant serverTime,
        List<Koltuk> seats,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalAmount) {

    public record Koltuk(Long cellId, String label) {
    }
}
