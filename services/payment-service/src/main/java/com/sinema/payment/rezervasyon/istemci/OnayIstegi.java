package com.sinema.payment.rezervasyon.istemci;

/** booking'e "ödeme başarılı, onayla"; paymentId booking'de iz sürmek için loglanır. */
public record OnayIstegi(Long paymentId) {
}
