package com.sinema.booking.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Booking'in iş kuralı değerleri (sinema.booking.*); koda gömülmez (planlama §6.4).
 *
 * @param holdSuresi              koltuk tutma süresi (K11: 3 dk); satış kapanışını aşamaz (§2.4-2)
 * @param enFazlaKoltuk           bir işlemde en çok koltuk (K24: 5)
 * @param odemePenceresi          ödeme başlatılınca hold'un bir kez uzadığı süre (K32: +2 dk)
 * @param terkKorumasi            K29: art arda terk edilen hold sonrası bekleme
 * @param dolulukOnbellekSuresi   seat-status cevabının bellekte tutulduğu süre (K47: 2 sn)
 */
@Validated
@ConfigurationProperties(prefix = "sinema.booking")
public record RezervasyonAyarlari(
        @NotNull Duration holdSuresi,
        @Min(1) int enFazlaKoltuk,
        @NotNull Duration odemePenceresi,
        @Valid @NotNull TerkKorumasi terkKorumasi,
        @NotNull Duration dolulukOnbellekSuresi) {

    /**
     * K29, K33: son {@code pencere} içinde {@code esik} kadar terk (süresi dolan ya da bırakılan hold) varsa,
     * son terkten {@code bekleme} sonrasına kadar yeni hold açılamaz.
     */
    public record TerkKorumasi(@Min(1) int esik, @NotNull Duration pencere, @NotNull Duration bekleme) {
    }
}
