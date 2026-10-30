package com.sinema.booking.katalog.istemci;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * catalog'un GET /internal/v1/catalog/showtimes/{id} cevabı (catalog IcSeansBilgisi). Feign DTO'su booking'in iç
 * modelinden ayrıdır (rules §5.7): catalog alan eklerse bilinmeyen alanlar yok sayılır, sözleşme kırılmaz.
 * ticketPrice JSON'da string gelir ("120.00"), BigDecimal'a kayıpsız okunur.
 */
public record KatalogSeansBilgisi(
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
        BigDecimal ticketPrice) {
}
