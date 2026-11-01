package com.sinema.booking.goc.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sinema.booking.goc.entity.GocNedeni;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class KoltukGocPlanlayiciTest {

    private static final long SEANS = 1;
    private final KoltukGocPlanlayici planlayici = new KoltukGocPlanlayici();

    /**
     * Metinle düzen kurar: her satır bir ızgara satırı. P = perde, K = koltuk, E = engelli koltuk, diğer = boşluk.
     * Hücre kimliği = taban + satır * 100 + sütun; koltuk etiketleri perdeden itibaren A, B… ve soldan sağa numara.
     */
    private static GocDuzeni duzen(long taban, String... satirlar) {
        List<GocHucresi> hucreler = new ArrayList<>();
        char sira = 'A';
        for (int satir = 0; satir < satirlar.length; satir++) {
            int numara = 1;
            boolean koltukVar = false;
            for (int sutun = 0; sutun < satirlar[satir].length(); sutun++) {
                char hucre = satirlar[satir].charAt(sutun);
                long id = taban + satir * 100L + sutun;
                if (hucre == 'P') {
                    hucreler.add(GocHucresi.perde(id, satir, sutun));
                } else if (hucre == 'K' || hucre == 'E') {
                    hucreler.add(GocHucresi.koltuk(id, satir, sutun, sira + "-" + numara++, hucre == 'E'));
                    koltukVar = true;
                }
            }
            if (koltukVar) {
                sira++;
            }
        }
        return new GocDuzeni(taban, hucreler);
    }

    private static long hucre(long taban, int satir, int sutun) {
        return taban + satir * 100L + sutun;
    }

    private static KoltukTasima tasimasi(SeansGocPlani plan, long eskiHucre) {
        return plan.tasimalar().stream().filter(t -> t.eskiHucreId() == eskiHucre).findFirst().orElseThrow();
    }

    @Test
    void shouldKeepSeatSilently_whenSamePositionAndLabelUnchanged() {
        GocDuzeni eski = duzen(1000, "PPP", "KKK", "KKK");
        GocDuzeni yeni = duzen(2000, "PPP", "KKK", "KK.");

        SeansGocPlani plan = planlayici.planla(SEANS, eski, yeni, List.of(new DoluKoltuk(7, hucre(1000, 2, 1))));

        assertThat(plan.uygulanabilir()).isTrue();
        assertThat(tasimasi(plan, hucre(1000, 2, 1)))
                .isEqualTo(new KoltukTasima(7, hucre(1000, 2, 1), "B-2", hucre(2000, 2, 1), "B-2", GocNedeni.SAME_POSITION));
    }

    @Test
    void shouldReportLabelChange_whenRowAboveRemovedButPositionStays() {
        GocDuzeni eski = duzen(1000, "PPP", "KKK", "KKK");
        GocDuzeni yeni = duzen(2000, "PPP", "...", "KKK");

        SeansGocPlani plan = planlayici.planla(SEANS, eski, yeni, List.of(new DoluKoltuk(7, hucre(1000, 2, 1))));

        KoltukTasima tasima = tasimasi(plan, hucre(1000, 2, 1));
        assertThat(tasima.neden()).isEqualTo(GocNedeni.LABEL_CHANGED);
        assertThat(tasima.eskiEtiket()).isEqualTo("B-2");
        assertThat(tasima.yeniEtiket()).isEqualTo("A-2");
        assertThat(tasima.neden().bildirimGerekir()).isTrue();
    }

    @Test
    void shouldMoveToNearestFreeSeat_whenSeatRemoved() {
        GocDuzeni eski = duzen(1000, "PPPPP", "KKKKK", "KKKKK");
        GocDuzeni yeni = duzen(2000, "PPPPP", "KK.KK", "KKKKK");
        List<DoluKoltuk> dolular = List.of(new DoluKoltuk(7, hucre(1000, 1, 2)), new DoluKoltuk(8, hucre(1000, 2, 2)));

        SeansGocPlani plan = planlayici.planla(SEANS, eski, yeni, dolular);

        // (2,2) aynı konumda kaldığı için 8'in; 7 için en yakın boşlar (1,1) ve (1,3): uzaklık ve perde ortasına
        // uzaklık eşit, son ölçüt sütun → soldaki
        assertThat(tasimasi(plan, hucre(1000, 2, 2)).neden()).isEqualTo(GocNedeni.SAME_POSITION);
        KoltukTasima yedek = tasimasi(plan, hucre(1000, 1, 2));
        assertThat(yedek.neden()).isEqualTo(GocNedeni.REPLACEMENT);
        assertThat(yedek.yeniHucreId()).isEqualTo(hucre(2000, 1, 1));
    }

    @Test
    void shouldPlaceAccessibleTicketOnAccessibleSeat_andKeepStandardOffAccessible() {
        GocDuzeni eski = duzen(1000, "PPPPP", "E....", "KKKKK");
        GocDuzeni yeni = duzen(2000, "PPPPP", ".....", "KKKKE");
        List<DoluKoltuk> dolular = List.of(new DoluKoltuk(7, hucre(1000, 1, 0)));

        SeansGocPlani plan = planlayici.planla(SEANS, eski, yeni, dolular);

        assertThat(tasimasi(plan, hucre(1000, 1, 0)).yeniHucreId()).isEqualTo(hucre(2000, 2, 4));

        GocDuzeni eskiStandart = duzen(1000, "PPPPP", "K....", "KKKKK");
        GocDuzeni yeniEngelliYakin = duzen(2000, "PPPPP", ".....", "EKKKK");
        SeansGocPlani standart = planlayici.planla(SEANS, eskiStandart, yeniEngelliYakin, dolular);
        assertThat(tasimasi(standart, hucre(1000, 1, 0)).yeniHucreId()).isEqualTo(hucre(2000, 2, 1));
    }

    @Test
    void shouldKeepReservationTogether_inAdjacentSeatsNotSplitByAisle() {
        GocDuzeni eski = duzen(1000, "PPPPPPP", "KKK.KKK", "KKKKKKK");
        GocDuzeni yeni = duzen(2000, "PPPPPPP", ".......", "KK.KK.K", "KKKKKKK");
        List<DoluKoltuk> dolular = List.of(new DoluKoltuk(7, hucre(1000, 1, 0)), new DoluKoltuk(7, hucre(1000, 1, 1)),
                new DoluKoltuk(7, hucre(1000, 1, 2)));

        SeansGocPlani plan = planlayici.planla(SEANS, eski, yeni, dolular);

        // 2. satırda 3 bitişik boş yok (koridorlar); 3. satırın en soldaki üçlüsü eski konumlara en yakın
        assertThat(plan.tasimalar()).extracting(KoltukTasima::yeniHucreId)
                .containsExactlyInAnyOrder(hucre(2000, 3, 0), hucre(2000, 3, 1), hucre(2000, 3, 2));
        assertThat(plan.tasimalar()).allMatch(t -> t.neden() == GocNedeni.REPLACEMENT);
    }

    @Test
    void shouldNotGiveAwaySamePositionSeat_asReplacementForAnotherReservation() {
        GocDuzeni eski = duzen(1000, "PPP", "KKK", "KK.");
        GocDuzeni yeni = duzen(2000, "PPP", "...", "KKK");
        // 7'nin koltuğu (1,1) kalktı; en yakın yer (2,1) ama orası 8'in aynı konumu
        List<DoluKoltuk> dolular = List.of(new DoluKoltuk(7, hucre(1000, 1, 1)), new DoluKoltuk(8, hucre(1000, 2, 1)));

        SeansGocPlani plan = planlayici.planla(SEANS, eski, yeni, dolular);

        assertThat(tasimasi(plan, hucre(1000, 2, 1)).yeniHucreId()).isEqualTo(hucre(2000, 2, 1));
        assertThat(tasimasi(plan, hucre(1000, 1, 1)).yeniHucreId()).isIn(hucre(2000, 2, 0), hucre(2000, 2, 2));
    }

    @Test
    void shouldReportUnresolvedReservation_andStillPlanOthers() {
        GocDuzeni eski = duzen(1000, "PPPP", "KKKK", "KKKK");
        GocDuzeni yeni = duzen(2000, "PPPP", "....", "KK.K");
        List<DoluKoltuk> dolular = List.of(new DoluKoltuk(7, hucre(1000, 1, 0)), new DoluKoltuk(7, hucre(1000, 1, 1)),
                new DoluKoltuk(7, hucre(1000, 1, 2)), new DoluKoltuk(8, hucre(1000, 2, 3)));

        SeansGocPlani plan = planlayici.planla(SEANS, eski, yeni, dolular);

        assertThat(plan.uygulanabilir()).isFalse();
        assertThat(plan.cozulemeyenler()).containsExactly(new CozulemeyenRezervasyon(7, List.of("A-1", "A-2", "A-3")));
        assertThat(tasimasi(plan, hucre(1000, 2, 3)).neden()).isEqualTo(GocNedeni.LABEL_CHANGED);
    }

    @Test
    void shouldPreferBlockNearerScreenCenter_whenDistancesTie() {
        GocDuzeni eski = duzen(1000, "..PPP..", "...K...", "K.....K");
        GocDuzeni yeni = duzen(2000, "..PPP..", ".......", "K.....K");
        // (1,3) kalktı; (2,0) ve (2,6) eski konuma eşit uzaklıkta. Perde ortası (sütun 3) ikisine de eşit uzak →
        // son ölçüt sütun: soldaki. Perde sağa kayınca (ortası 5) sağdaki perdeye daha yakın olur.
        List<DoluKoltuk> dolular = List.of(new DoluKoltuk(7, hucre(1000, 1, 3)));
        assertThat(tasimasi(planlayici.planla(SEANS, eski, yeni, dolular), hucre(1000, 1, 3)).yeniHucreId())
                .isEqualTo(hucre(2000, 2, 0));

        GocDuzeni yeniSagPerde = duzen(2000, "....PPP", ".......", "K.....K");
        assertThat(tasimasi(planlayici.planla(SEANS, eski, yeniSagPerde, dolular), hucre(1000, 1, 3)).yeniHucreId())
                .isEqualTo(hucre(2000, 2, 6));
    }

    @Test
    void shouldFailFast_whenOccupiedSeatNotInShowtimeLayout() {
        GocDuzeni eski = duzen(1000, "PP", "KK");
        GocDuzeni yeni = duzen(2000, "PP", "KK");

        assertThatThrownBy(() -> planlayici.planla(SEANS, eski, yeni, List.of(new DoluKoltuk(7, 999_999))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("999999");
    }
}
