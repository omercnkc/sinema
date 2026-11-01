package com.sinema.booking.goc.plan;

/** Seansta dolu bir koltuk (SOLD ya da süresi dolmamış HELD; K51: ikisi de taşınır) ve sahibi rezervasyon. */
public record DoluKoltuk(long rezervasyonId, long hucreId) {
}
