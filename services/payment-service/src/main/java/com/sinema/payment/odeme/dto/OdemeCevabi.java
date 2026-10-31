package com.sinema.payment.odeme.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sinema.payment.odeme.entity.OdemeDurumu;
import java.math.BigDecimal;

/**
 * Ödeme cevabı (planlama §7.6). paymentPageUrl yalnızca ödeme sayfası hâlâ kullanılabilirken dolu; ön yüz tarayıcıyı
 * bu adrese tam sayfa yönlendirir (K41). Para string ("300.00").
 */
public record OdemeCevabi(
        Long paymentId,
        Long reservationId,
        OdemeDurumu status,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        String paymentPageUrl) {
}
