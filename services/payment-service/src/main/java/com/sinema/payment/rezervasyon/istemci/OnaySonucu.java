package com.sinema.payment.rezervasyon.istemci;

/** booking onay cevabı; aynı onay tekrar gelirse aynı bilet kodu döner. */
public record OnaySonucu(Long reservationId, String status, String ticketCode) {
}
