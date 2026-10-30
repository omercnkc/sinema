package com.sinema.booking.rezervasyon.dto;

import com.sinema.booking.rezervasyon.entity.RezervasyonDurumu;

/** Onay sonucu; aynı onay tekrar gelirse aynı bilet kodu döner (idempotent). */
public record OnayCevabi(Long reservationId, RezervasyonDurumu status, String ticketCode) {
}
