package com.sinema.booking.rezervasyon.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** payment → booking "ödeme başarılı, onayla" (§4.5). paymentId iz sürmek için loglanır. */
public record OnayIstegi(@NotNull @Positive Long paymentId) {
}
