package com.sinema.catalog.duzen.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sinema.catalog.duzen.dto.DuzenIstegi;
import com.sinema.catalog.duzen.dto.HucreIstegi;
import com.sinema.catalog.duzen.entity.HucreTipi;
import com.sinema.catalog.duzen.entity.KoltukTipi;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DuzenDogrulayiciTest {

    private final DuzenDogrulayici dogrulayici = new DuzenDogrulayici();

    @Test
    void shouldReturnNoViolations_whenLayoutHasScreenAndUniqueSeats() {
        assertThat(dogrulayici.ihlalleriBul(duzen(perde(0, 0), koltuk(1, 0, "A", 1), koltuk(1, 1, "A", 2))))
                .isEmpty();
    }

    @Test
    void shouldReportCell_whenOutsideGrid() {
        assertThat(dogrulayici.ihlalleriBul(duzen(perde(0, 0), koltuk(3, 0, "A", 1))))
                .containsExactly("(3,0): hücre ızgaranın dışında");
    }

    @Test
    void shouldReportDuplicatePosition_whenTwoCellsShareCoordinates() {
        assertThat(dogrulayici.ihlalleriBul(duzen(perde(0, 0), koltuk(1, 1, "A", 1), duvar(1, 1))))
                .containsExactly("(1,1): aynı konumda birden fazla hücre var");
    }

    @Test
    void shouldReportDuplicateLabel_whenTwoSeatsShareRowAndNumber() {
        assertThat(dogrulayici.ihlalleriBul(duzen(perde(0, 0), koltuk(1, 0, "A", 1), koltuk(2, 0, "A", 1))))
                .containsExactly("(2,0): A-1 etiketi başka bir koltukta da var");
    }

    @Test
    void shouldRequireSeatFields_whenCellIsSeat() {
        HucreIstegi eksikKoltuk = new HucreIstegi(1, 0, HucreTipi.SEAT, "A", null, KoltukTipi.STANDARD);

        assertThat(dogrulayici.ihlalleriBul(duzen(perde(0, 0), koltuk(1, 1, "A", 1), eksikKoltuk)))
                .containsExactly("(1,0): koltukta sıra etiketi, numara ve koltuk tipi zorunlu");
    }

    @Test
    void shouldRejectSeatFields_whenCellIsNotSeat() {
        HucreIstegi etiketliDuvar = new HucreIstegi(2, 2, HucreTipi.WALL, "A", null, null);

        assertThat(dogrulayici.ihlalleriBul(duzen(perde(0, 0), koltuk(1, 0, "A", 1), etiketliDuvar)))
                .containsExactly("(2,2): koltuk olmayan hücrede koltuk bilgisi olamaz");
    }

    @Test
    void shouldRequireSeatAndScreen_whenLayoutHasNeither() {
        assertThat(dogrulayici.ihlalleriBul(duzen(duvar(0, 0))))
                .containsExactly("Düzende en az bir koltuk olmalı", "Düzende perde (SCREEN) olmalı");
    }

    @Test
    void shouldReportAllViolations_whenSeveralRulesBroken() {
        assertThat(dogrulayici.ihlalleriBul(duzen(koltuk(5, 5, "A", 1), koltuk(1, 0, "A", 1)))).hasSize(3);
    }

    private static DuzenIstegi duzen(HucreIstegi... hucreler) {
        return new DuzenIstegi(3, 3, new ArrayList<>(List.of(hucreler)));
    }

    private static HucreIstegi koltuk(int satir, int sutun, String sira, int numara) {
        return new HucreIstegi(satir, sutun, HucreTipi.SEAT, sira, numara, KoltukTipi.STANDARD);
    }

    private static HucreIstegi perde(int satir, int sutun) {
        return new HucreIstegi(satir, sutun, HucreTipi.SCREEN, null, null, null);
    }

    private static HucreIstegi duvar(int satir, int sutun) {
        return new HucreIstegi(satir, sutun, HucreTipi.WALL, null, null, null);
    }
}
