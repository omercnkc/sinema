package com.sinema.booking.bilet.dto;

import java.time.Instant;
import java.util.List;

/**
 * Herkese açık bilet doğrulama cevabı (K30, planlama §7.5): QR okutulunca biletin geçerli olduğu ve bilet üzerindeki
 * bilgiler. Kişisel veri yok: kullanıcı kimliği, e-posta, ad, tutar dönmez.
 */
public record BiletDogrulamaCevabi(
        boolean valid,
        String movieTitle,
        String cinemaName,
        String hallName,
        Instant startsAt,
        List<String> seats) {
}
