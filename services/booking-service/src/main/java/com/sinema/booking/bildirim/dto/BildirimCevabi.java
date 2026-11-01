package com.sinema.booking.bildirim.dto;

import com.sinema.booking.bildirim.entity.BildirimTuru;
import java.time.Instant;

/** Uygulama içi bildirim; {@code reservationId} varsa ön yüz ilgili bilete bağlantı verir. */
public record BildirimCevabi(Long id, BildirimTuru type, Long reservationId, String message, boolean read,
        Instant createdAt) {
}
