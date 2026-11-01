package com.sinema.catalog.seans.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Admin seans çizelgesindeki satır (salon + gün). Gizli filmlerin seansları da listelenir (movieVisible ile
 * işaretli, K40). layoutId seansın mühürlü düzenidir; salonun aktif düzeninden farklıysa seans eski sürümdedir (K17).
 */
public record YonetimSeansOzeti(
        Long id,
        Long movieId,
        String movieTitle,
        boolean movieVisible,
        Instant startsAt,
        Instant endsAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal ticketPrice,
        int salesCloseOffsetMinutes,
        Instant salesCloseAt,
        boolean salesOpen,
        Long layoutId) {
}
