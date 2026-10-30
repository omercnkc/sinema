package com.sinema.booking.rezervasyon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sinema.booking.rezervasyon.entity.RezervasyonDurumu;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Rezervasyon cevabı (planlama §7.5); tutma, tekil sorgu ve liste aynı biçimi kullanır. Geri sayım expiresAt ve
 * serverTime farkından hesaplanır, istemci saatine güvenilmez (§4.3). Para string ("150.00"), kayan nokta kaybı
 * yok (§7.2). Film/şube/salon/saat oluşturma anının kopyasıdır (§3.4-1). ticketCode yalnızca CONFIRMED'da dolu.
 */
public record RezervasyonCevabi(
        Long reservationId,
        Long showtimeId,
        RezervasyonDurumu status,
        String movieTitle,
        String cinemaName,
        String hallName,
        Instant startsAt,
        Instant expiresAt,
        Instant serverTime,
        List<Koltuk> seats,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalAmount,
        String ticketCode) {

    public record Koltuk(Long cellId, String label) {
    }
}
