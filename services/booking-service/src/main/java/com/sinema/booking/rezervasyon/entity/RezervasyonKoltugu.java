package com.sinema.booking.rezervasyon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Rezervasyondaki bir koltuk (reservation_seats). Etiket ve fiyat o anki değerin kopyasıdır: catalog'da fiyat ya da
 * etiket sonradan değişse bile bilet değişmez. Kimliği hücre id'sidir (aynı rezervasyonda bir hücre bir kez).
 */
@Embeddable
@Getter
@EqualsAndHashCode(of = "hucreId")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RezervasyonKoltugu {

    @Column(name = "layout_cell_id", nullable = false)
    private Long hucreId;

    @Column(name = "seat_label", nullable = false, length = 10)
    private String etiket;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal birimFiyat;

    public RezervasyonKoltugu(Long hucreId, String etiket, BigDecimal birimFiyat) {
        this.hucreId = hucreId;
        this.etiket = etiket;
        this.birimFiyat = birimFiyat;
    }
}
