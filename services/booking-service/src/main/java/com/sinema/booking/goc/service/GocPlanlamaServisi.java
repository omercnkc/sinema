package com.sinema.booking.goc.service;

import com.sinema.booking.goc.plan.DoluKoltuk;
import com.sinema.booking.goc.plan.GocDuzeni;
import com.sinema.booking.goc.plan.GocHucresi;
import com.sinema.booking.goc.plan.KoltukGocPlanlayici;
import com.sinema.booking.goc.plan.SeansGocPlani;
import com.sinema.booking.katalog.istemci.KatalogGocKaynagi;
import com.sinema.booking.katalog.istemci.KatalogGocKaynagi.DuzenHucreleri;
import com.sinema.booking.katalog.istemci.KatalogGocKaynagi.Hucre;
import com.sinema.booking.katalog.istemci.KatalogIstemcisi;
import com.sinema.booking.koltuk.repository.KoltukKaydiRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Göç planını hesaplar (K35): catalog'dan hedef taslağı ve başlamamış seansları okur (Feign, servis token'ı),
 * her seansın dolu koltuklarını booking'den alır ve planlayıcıdan geçirir. Hiçbir şey yazmaz.
 */
@Service
@RequiredArgsConstructor
public class GocPlanlamaServisi {

    private static final String KOLTUK = "SEAT";
    private static final String PERDE = "SCREEN";
    private static final String ENGELLI = "ACCESSIBLE";

    private final KatalogIstemcisi katalogIstemcisi;
    private final KoltukKaydiRepository koltukKaydiRepository;
    private final KoltukGocPlanlayici planlayici;
    private final Clock saat;

    /**
     * catalog çağrısı transaction dışında (DB bağlantısı uzak çağrı boyunca tutulmaz). Seans okumaları tek tek
     * kısa sorgular; önizlemede seanslar arası anlık tutarlılık gerekmez, uygulama planı kilit altında yeniden hesaplar.
     */
    public GocHesabi hesapla(Long salonId, Long hedefDuzenId) {
        KatalogGocKaynagi kaynak = katalogIstemcisi.gocKaynagi(salonId, hedefDuzenId);
        return planla(kaynak);
    }

    public GocHesabi planla(KatalogGocKaynagi kaynak) {
        GocDuzeni yeniDuzen = gocDuzeni(kaynak.targetLayout());
        Map<Long, GocDuzeni> eskiDuzenler = kaynak.sourceLayouts().stream()
                .map(GocPlanlamaServisi::gocDuzeni)
                .collect(Collectors.toMap(GocDuzeni::duzenId, Function.identity()));
        Instant simdi = saat.instant();
        List<SeansGocPlani> planlar = new ArrayList<>();
        for (KatalogGocKaynagi.Seans seans : kaynak.showtimes()) {
            List<DoluKoltuk> dolular = koltukKaydiRepository.gocIcinDoluKoltuklar(seans.id(), simdi);
            if (!dolular.isEmpty()) {
                GocDuzeni eskiDuzen = Objects.requireNonNull(eskiDuzenler.get(seans.layoutId()),
                        () -> "catalog seansın düzenini göndermedi: " + seans.layoutId());
                planlar.add(planlayici.planla(seans.id(), eskiDuzen, yeniDuzen, dolular));
            }
        }
        return new GocHesabi(kaynak, planlar);
    }

    private static GocDuzeni gocDuzeni(DuzenHucreleri duzen) {
        List<GocHucresi> hucreler = duzen.cells().stream()
                .filter(hucre -> KOLTUK.equals(hucre.type()) || PERDE.equals(hucre.type()))
                .map(GocPlanlamaServisi::gocHucresi)
                .toList();
        return new GocDuzeni(duzen.layoutId(), hucreler);
    }

    private static GocHucresi gocHucresi(Hucre hucre) {
        if (PERDE.equals(hucre.type())) {
            return GocHucresi.perde(hucre.cellId(), hucre.row(), hucre.col());
        }
        // Etiket biçimi biletteki gibi (catalog SeansMapper.koltuk ile aynı): sıra harfi + "-" + numara
        return GocHucresi.koltuk(hucre.cellId(), hucre.row(), hucre.col(),
                hucre.seatRowLabel() + "-" + hucre.seatNumber(), ENGELLI.equals(hucre.seatType()));
    }
}
