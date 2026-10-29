package com.sinema.common.korelasyon;

/**
 * Bir isteği gateway'den son servise kadar loglarda takip etmeyi sağlayan kimliğin sabitleri.
 */
public final class KorelasyonKimligi {

    public static final String HTTP_BASLIGI = "X-Correlation-Id";
    public static final String MDC_ANAHTARI = "correlationId";

    private KorelasyonKimligi() {
    }
}
