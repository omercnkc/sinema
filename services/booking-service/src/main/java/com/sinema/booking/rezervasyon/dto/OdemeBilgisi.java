package com.sinema.booking.rezervasyon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sinema.booking.rezervasyon.entity.RezervasyonDurumu;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * payment-service için rezervasyon özeti (iç uç, planlama §7.5). Tutar burada sunucudan gelir; istemci tutar
 * gönderemez (§4.8). userId: payment, ödemeyi başlatan kullanıcının rezervasyonun sahibi olduğunu bununla doğrular.
 * Kullanıcıya dönen RezervasyonCevabi'ndan ayrı: iki sözleşme bağımsız değişebilsin.
 */
public record OdemeBilgisi(
        Long reservationId,
        Long userId,
        RezervasyonDurumu status,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalAmount,
        Instant expiresAt,
        boolean paymentWindowExtended,
        Instant serverTime) {
}
