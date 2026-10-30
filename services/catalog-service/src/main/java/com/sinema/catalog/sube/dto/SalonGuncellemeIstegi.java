package com.sinema.catalog.sube.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Salonun şubesi değiştirilemez (seanslar ve biletler şube adını salondan alır).
 * Fiyat değişikliği yalnızca bundan sonra oluşturulan seanslara kopyalanır (K23).
 */
public record SalonGuncellemeIstegi(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal defaultTicketPrice) {
}
