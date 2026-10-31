package com.sinema.payment.odeme.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** POST /api/v1/payments gövdesi (planlama §7.6). Tutar bilerek yok: booking'den alınır (§4.8). */
public record OdemeBaslatmaIstegi(@NotNull @Positive Long reservationId) {
}
