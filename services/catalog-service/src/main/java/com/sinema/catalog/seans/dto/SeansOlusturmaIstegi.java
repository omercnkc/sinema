package com.sinema.catalog.seans.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Seans ekleme (planlama §7.4). startsAt UTC ISO-8601 (ör. "2026-11-01T17:00:00Z").
 * Fiyat boşsa salonun varsayılanı (K23), ofset boşsa yapılandırmadaki varsayılan (K10) kullanılır.
 */
public record SeansOlusturmaIstegi(
        @NotNull Long movieId,
        @NotNull Long hallId,
        @NotNull Instant startsAt,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal ticketPrice,
        @Min(SeansOlusturmaIstegi.EN_KUCUK_OFSET) @Max(SeansOlusturmaIstegi.EN_BUYUK_OFSET)
        Integer salesCloseOffsetMinutes) {

    /** Satış en erken başlangıçtan 2 saat önce kapanabilir, en geç 2 saat sonra. */
    public static final int EN_KUCUK_OFSET = -120;
    public static final int EN_BUYUK_OFSET = 120;
}
