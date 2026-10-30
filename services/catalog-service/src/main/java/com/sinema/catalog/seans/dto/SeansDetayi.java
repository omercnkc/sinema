package com.sinema.catalog.seans.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Koltuk seçimi sayfasının başlığı: film, şube, salon, saat, fiyat ve satış durumu.
 * serverTime: istemci saatine güvenilmez, geri sayımlar sunucu saatine göre hizalanır (§4.3).
 */
public record SeansDetayi(
        Long id,
        Long movieId,
        String movieTitle,
        String posterPath,
        String certification,
        Long cinemaId,
        String cinemaName,
        Long hallId,
        String hallName,
        Long layoutId,
        Instant startsAt,
        Instant endsAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal ticketPrice,
        int salesCloseOffsetMinutes,
        Instant salesCloseAt,
        boolean salesOpen,
        Instant serverTime) {
}
