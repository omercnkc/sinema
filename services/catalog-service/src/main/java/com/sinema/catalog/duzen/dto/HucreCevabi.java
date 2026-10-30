package com.sinema.catalog.duzen.dto;

import com.sinema.catalog.duzen.entity.HucreTipi;
import com.sinema.catalog.duzen.entity.KoltukTipi;

/** Alan adları halka açık düzen ucuyla aynı (planlama §7.4): cellId, koltuğun kalıcı kimliği. */
public record HucreCevabi(
        Long cellId,
        int row,
        int col,
        HucreTipi type,
        String seatRowLabel,
        Integer seatNumber,
        KoltukTipi seatType) {
}
