package com.sinema.catalog.seans.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Yalnızca fiyat ve satış kapanış ofseti değişir; film, salon ve saat değişmez, seans silinmez (K34). */
public record SeansGuncellemeIstegi(
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal ticketPrice,
        @NotNull @Min(SeansOlusturmaIstegi.EN_KUCUK_OFSET) @Max(SeansOlusturmaIstegi.EN_BUYUK_OFSET)
        Integer salesCloseOffsetMinutes) {
}
