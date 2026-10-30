package com.sinema.booking.katalog.istemci;

import java.util.List;

/**
 * catalog'un koltuk çözümleme cevabı (catalog KoltukCozumlemeCevabi). invalidCellIds boş değilse hold açılmaz (§4.3).
 * seatType düz metin tutulur: booking yalnızca etiketi (bilet snapshot'ı) kullanır, catalog'un enum'una bağlanmaz.
 */
public record KoltukCozumlemeSonucu(Long layoutId, List<Koltuk> seats, List<Long> invalidCellIds) {

    public record Koltuk(Long cellId, String seatRowLabel, int seatNumber, String label, String seatType) {
    }
}
