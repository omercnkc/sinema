package com.sinema.catalog.seans.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

/** Film detayındaki seans satırı (planlama §7.4). salesOpen sunucu saatine göre hesaplanır. */
public record SeansOzeti(
        Long id,
        Instant startsAt,
        Instant endsAt,
        Long cinemaId,
        String cinemaName,
        Long hallId,
        String hallName,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal ticketPrice,
        Instant salesCloseAt,
        boolean salesOpen) {
}
