package com.sinema.booking.doluluk.dto;

import java.time.Instant;
import java.util.List;

/**
 * GET seat-status cevabı (planlama §4.9, §7.5): yalnızca dolu koltuklar; listede olmayan koltuk boştur.
 * Kimin tuttuğu yazılmaz (misafire açık uç). serverTime istemcinin saat farkını hesaplaması içindir.
 */
public record DolulukCevabi(Instant serverTime, List<DoluKoltuk> occupied) {

    public DolulukCevabi {
        occupied = List.copyOf(occupied);
    }
}
