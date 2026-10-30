package com.sinema.catalog.sube.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

/** Para string olarak yazılır ("120.00"): istemcide kayan nokta kaybı olmaz (planlama §7.2). */
public record SalonCevabi(
        Long id,
        Long cinemaId,
        String name,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal defaultTicketPrice,
        Long activeLayoutId) {
}
