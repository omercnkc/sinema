package com.sinema.catalog.sube.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Fiyat JSON'da string ("120.00") veya sayı olarak gelebilir; en çok 2 ondalık basamak (K23, planlama §7.2). */
public record SalonOlusturmaIstegi(
        @NotNull Long cinemaId,
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal defaultTicketPrice) {
}
