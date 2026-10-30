package com.sinema.catalog.duzen.dto;

import com.sinema.catalog.duzen.entity.HucreTipi;
import com.sinema.catalog.duzen.entity.KoltukTipi;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Bir hücre. Koltuk alanları (sıra etiketi, numara, tip) yalnızca SEAT için gönderilir;
 * bunun kontrolü DuzenDogrulayici'da (alanlar arası kural).
 */
public record HucreIstegi(
        @NotNull @Min(0) Integer row,
        @NotNull @Min(0) Integer col,
        @NotNull HucreTipi type,
        @Pattern(regexp = "[A-Z]{1,3}", message = "1-3 büyük harf olmalı (ör. A, F, AA)") String seatRowLabel,
        @Min(1) @Max(999) Integer seatNumber,
        KoltukTipi seatType) {
}
