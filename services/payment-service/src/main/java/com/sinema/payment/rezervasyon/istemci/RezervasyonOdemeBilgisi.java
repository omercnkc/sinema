package com.sinema.payment.rezervasyon.istemci;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * booking'in payment-info / extend-payment-window cevabı (booking OdemeBilgisi). Feign DTO'su payment'ın iç
 * modelinden ayrıdır (rules §5.7). status düz metin: booking yeni durum eklerse sözleşme kırılmaz.
 */
public record RezervasyonOdemeBilgisi(
        Long reservationId,
        Long userId,
        String status,
        BigDecimal totalAmount,
        Instant expiresAt,
        boolean paymentWindowExtended,
        Instant serverTime) {

    public static final String ODEME_BEKLIYOR = "PENDING_PAYMENT";

    public boolean odemeBekliyor() {
        return ODEME_BEKLIYOR.equals(status);
    }
}
