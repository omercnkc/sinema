package com.sinema.catalog.ornekveri;

import static org.assertj.core.api.Assertions.assertThat;

import com.sinema.catalog.duzen.dto.DuzenIstegi;
import com.sinema.catalog.duzen.dto.HucreIstegi;
import com.sinema.catalog.duzen.entity.HucreTipi;
import com.sinema.catalog.duzen.entity.KoltukTipi;
import com.sinema.catalog.duzen.service.DuzenDogrulayici;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class OrnekDuzenUreticiTest {

    @Test
    void shouldPassLayoutRules_forEveryHallInSampleData() {
        DuzenDogrulayici dogrulayici = new DuzenDogrulayici();

        OrnekVeriler.SUBELER.stream().flatMap(sube -> sube.salonlar().stream()).forEach(salon ->
                assertThat(dogrulayici.ihlalleriBul(OrnekDuzenUretici.uret(salon.duzen())))
                        .as(salon.ad()).isEmpty());
    }

    @Test
    void shouldProduceTenDistinctLayouts_whenSampleDataGenerated() {
        Set<String> imzalar = OrnekVeriler.SUBELER.stream()
                .flatMap(sube -> sube.salonlar().stream())
                .map(salon -> OrnekDuzenUretici.uret(salon.duzen()))
                .map(duzen -> duzen.gridRows() + "x" + duzen.gridCols() + ":" + koltuklar(duzen).size())
                .collect(Collectors.toSet());

        assertThat(imzalar).hasSize(10);
    }

    @Test
    void shouldPlaceScreenAisleAccessibleSeatsAndDoors_whenTemplateHasOneAisle() {
        // 3 sıra x 4 koltuk, 2. koltuktan sonra koridor, giriş solda
        DuzenIstegi duzen = OrnekDuzenUretici.uret(new DuzenSablonu(3, 4, List.of(2), true, 0));

        assertThat(duzen.gridRows()).isEqualTo(6);
        assertThat(duzen.gridCols()).isEqualTo(7);
        assertThat(hucre(duzen, 0, 1).type()).isEqualTo(HucreTipi.SCREEN);
        assertThat(koltuklar(duzen)).hasSize(12);
        assertThat(sutunlar(duzen, 2)).as("ilk sıra: koridor sütunu 3 boş").containsExactly(1, 2, 4, 5);
        assertThat(hucre(duzen, 4, 1).seatRowLabel()).isEqualTo("C");
        assertThat(hucre(duzen, 4, 1).seatType()).isEqualTo(KoltukTipi.ACCESSIBLE);
        assertThat(hucre(duzen, 4, 4).seatNumber()).isEqualTo(3);
        assertThat(hucre(duzen, 5, 0).type()).isEqualTo(HucreTipi.ENTRANCE);
        assertThat(hucre(duzen, 5, 6).type()).isEqualTo(HucreTipi.EXIT);
    }

    @Test
    void shouldSkipEndSeatsAndRenumber_whenFrontCornersMissing() {
        DuzenIstegi duzen = OrnekDuzenUretici.uret(new DuzenSablonu(2, 5, List.of(), false, 1));

        assertThat(sutunlar(duzen, 2)).as("A sırası: uçlar yok").containsExactly(2, 3, 4);
        assertThat(hucre(duzen, 2, 2).seatNumber()).isEqualTo(1);
        assertThat(sutunlar(duzen, 3)).as("B sırası tam").containsExactly(1, 2, 3, 4, 5);
        assertThat(hucre(duzen, 4, 0).type()).as("giriş sağda, çıkış solda").isEqualTo(HucreTipi.EXIT);
    }

    private static List<HucreIstegi> koltuklar(DuzenIstegi duzen) {
        return duzen.cells().stream().filter(h -> h.type() == HucreTipi.SEAT).toList();
    }

    private static List<Integer> sutunlar(DuzenIstegi duzen, int satir) {
        return koltuklar(duzen).stream().filter(h -> h.row() == satir).map(HucreIstegi::col).sorted().toList();
    }

    private static HucreIstegi hucre(DuzenIstegi duzen, int satir, int sutun) {
        return duzen.cells().stream().filter(h -> h.row() == satir && h.col() == sutun).findFirst().orElseThrow();
    }
}
