package com.sinema.catalog.duzen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Düzenin boş olmayan bir hücresi (layout_cells). Koltuk ise id'si koltuğun kalıcı kimliğidir:
 * booking koltuk tutarken bu id'yi kullanır (planlama §2.2).
 */
@Entity
@Table(name = "layout_cells")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DuzenHucresi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "layout_id", nullable = false, updatable = false)
    private Long duzenId;

    @Column(name = "row_index", nullable = false, updatable = false)
    private int satir;

    @Column(name = "col_index", nullable = false, updatable = false)
    private int sutun;

    @Enumerated(EnumType.STRING)
    @Column(name = "cell_type", nullable = false, length = 20, updatable = false)
    private HucreTipi tip;

    @Column(name = "seat_row_label", length = 5, updatable = false)
    private String siraEtiketi;

    @Column(name = "seat_number", updatable = false)
    private Integer koltukNumarasi;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", length = 20, updatable = false)
    private KoltukTipi koltukTipi;

    private DuzenHucresi(Long duzenId, int satir, int sutun, HucreTipi tip) {
        this.duzenId = duzenId;
        this.satir = satir;
        this.sutun = sutun;
        this.tip = tip;
    }

    public static DuzenHucresi koltuk(Long duzenId, int satir, int sutun, String siraEtiketi, int koltukNumarasi,
            KoltukTipi koltukTipi) {
        DuzenHucresi hucre = new DuzenHucresi(duzenId, satir, sutun, HucreTipi.SEAT);
        hucre.siraEtiketi = siraEtiketi;
        hucre.koltukNumarasi = koltukNumarasi;
        hucre.koltukTipi = koltukTipi;
        return hucre;
    }

    public static DuzenHucresi koltukDisi(Long duzenId, int satir, int sutun, HucreTipi tip) {
        if (tip == HucreTipi.SEAT) {
            throw new IllegalArgumentException("Koltuk için koltuk(...) kullanılmalı");
        }
        return new DuzenHucresi(duzenId, satir, sutun, tip);
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof DuzenHucresi hucre && id != null && id.equals(hucre.id);
    }

    @Override
    public int hashCode() {
        return DuzenHucresi.class.hashCode();
    }
}
