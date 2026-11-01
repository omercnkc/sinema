package com.sinema.booking.goc.service;

import com.sinema.booking.goc.dto.GocRaporu;
import com.sinema.booking.goc.dto.GocSonucu;
import com.sinema.booking.goc.entity.DuzenGocu;
import com.sinema.booking.goc.entity.GocDurumu;
import com.sinema.booking.goc.plan.SeansGocPlani;
import com.sinema.booking.goc.repository.BakimKilidiRepository;
import com.sinema.booking.goc.repository.DuzenGocuRepository;
import com.sinema.booking.katalog.istemci.KatalogEtkinlestirmeIstegi;
import com.sinema.booking.katalog.istemci.KatalogEtkinlestirmeSonucu;
import com.sinema.booking.katalog.istemci.KatalogIstemcisi;
import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.SinemaIstisnasi;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Düzen göçünü uygular (K35, §7.8). Bilerek @Transactional değil: catalog çağrıları adımlar arasında, transaction
 * dışında yapılır. Sıra: göç kaydı + salon kilidi (commit) → plan (kilit altında yeniden hesaplanır; önizlemeden
 * sonra satış olmuş olabilir) → biletler taşınır + denetim kaydı (commit) → catalog activate → tamamla
 * (bildirimler, kilit kalkar).
 *
 * catalog activate hatası: net iş hatası (409/422/404) catalog'da hiçbir şeyin değişmediği anlamına gelir →
 * biletler denetim kaydından geri alınır. Belirsiz hata (zaman aşımı, 503) catalog'un etkinleştirmiş olabileceği
 * anlamına gelir → körlemesine geri alınmaz: göç askıya alınır, salon kilitli kalır; aynı göç tekrar uygulanınca
 * yalnızca activate yeniden denenir (catalog tarafında idempotent).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GocUygulamaServisi {

    public static final String GOC_SURUYOR_KODU = "MIGRATION_IN_PROGRESS";
    public static final String GOC_UYGULANAMAZ_KODU = "MIGRATION_NOT_APPLICABLE";
    public static final String GOC_ASKIDA_KODU = "MIGRATION_PENDING";
    private static final String BEKLENMEYEN_HATA_KODU = "UNEXPECTED";

    private final GocIslemleri islemler;
    private final GocPlanlamaServisi planlamaServisi;
    private final GocRaporuOlusturucu raporOlusturucu;
    private final KatalogIstemcisi katalogIstemcisi;
    private final BakimKilidiRepository kilitRepository;
    private final DuzenGocuRepository gocRepository;
    private final Clock saat;

    public GocSonucu uygula(Long salonId, Long hedefDuzenId, Long kullaniciId) {
        Optional<DuzenGocu> askidaki = askidakiGoc(salonId);
        if (askidaki.isPresent()) {
            return surdur(askidaki.get(), hedefDuzenId);
        }
        DuzenGocu gocu = kilitle(salonId, hedefDuzenId, kullaniciId);
        GocHesabi hesap = vazgecerek(gocu, () -> planlamaServisi.hesapla(salonId, hedefDuzenId));
        GocRaporu rapor = raporOlusturucu.rapor(salonId, hedefDuzenId, hesap);
        if (!hesap.uygulanabilir()) {
            islemler.vazgec(gocu.getId(), GOC_UYGULANAMAZ_KODU, saat.instant());
            throw new CakismaIstisnasi(GOC_UYGULANAMAZ_KODU,
                    "Bazı biletler yeni düzende yan yana yerleştirilemiyor; düzeni düzeltip tekrar deneyin.",
                    Map.of("report", rapor));
        }
        vazgecerek(gocu, () -> {
            islemler.biletleriTasi(gocu.getId(), hesap, saat.instant());
            return null;
        });
        List<Long> seanslar = hesap.planlar().stream().map(SeansGocPlani::seansId).toList();
        return etkinlestirVeTamamla(gocu, seanslar, rapor);
    }

    private Optional<DuzenGocu> askidakiGoc(Long salonId) {
        return kilitRepository.findById(salonId).map(kilit -> gocRepository.findById(kilit.getGocId()).orElseThrow());
    }

    private GocSonucu surdur(DuzenGocu gocu, Long hedefDuzenId) {
        if (!gocu.surdurulebilir(hedefDuzenId)) {
            throw new CakismaIstisnasi(GOC_SURUYOR_KODU, "Bu salonda süren bir düzen göçü var; biraz sonra tekrar deneyin.");
        }
        log.info("Askıdaki göç {} sürdürülüyor (salon {})", gocu.getId(), gocu.getSalonId());
        return etkinlestirVeTamamla(gocu, islemler.tasinanSeanslar(gocu.getId()), null);
    }

    private DuzenGocu kilitle(Long salonId, Long hedefDuzenId, Long kullaniciId) {
        try {
            return islemler.kilitleVeBasla(salonId, hedefDuzenId, kullaniciId, saat.instant());
        } catch (DuplicateKeyException e) {
            throw new CakismaIstisnasi(GOC_SURUYOR_KODU, "Bu salonda süren bir düzen göçü var; biraz sonra tekrar deneyin.");
        }
    }

    /** Henüz hiçbir bilet değişmemişken hata: göç geri alınmış sayılır, kilit kalkar, hata olduğu gibi iletilir. */
    private <T> T vazgecerek(DuzenGocu gocu, Supplier<T> adim) {
        try {
            return adim.get();
        } catch (RuntimeException e) {
            islemler.vazgec(gocu.getId(), hataKodu(e), saat.instant());
            throw e;
        }
    }

    private GocSonucu etkinlestirVeTamamla(DuzenGocu gocu, List<Long> seanslar, GocRaporu rapor) {
        KatalogEtkinlestirmeSonucu sonuc;
        try {
            sonuc = katalogIstemcisi.etkinlestir(gocu.getSalonId(), gocu.getHedefDuzenId(),
                    new KatalogEtkinlestirmeIstegi(seanslar));
        } catch (BagimlilikKullanilamazIstisnasi e) {
            throw askiyaAl(gocu, e);
        } catch (SinemaIstisnasi e) {
            islemler.geriAl(gocu.getId(), e.getKod(), saat.instant());
            throw e;
        } catch (RuntimeException e) {
            throw askiyaAl(gocu, e);
        }
        islemler.tamamla(gocu.getId(), saat.instant());
        return new GocSonucu(gocu.getId(), GocDurumu.COMPLETED, sonuc.version(), sonuc.migratedShowtimeCount(), rapor);
    }

    private BagimlilikKullanilamazIstisnasi askiyaAl(DuzenGocu gocu, RuntimeException neden) {
        log.error("Göç {} askıya alındı: catalog etkinleştirmenin sonucu belirsiz ({}); salon {} kilitli kaldı",
                gocu.getId(), neden.toString(), gocu.getSalonId());
        islemler.askiyaAl(gocu.getId(), hataKodu(neden));
        return new BagimlilikKullanilamazIstisnasi(GOC_ASKIDA_KODU,
                "Yeni düzen etkinleştirilemedi; biletler korunuyor ve salon kilitli. Aynı göçü biraz sonra tekrar uygulayın.");
    }

    private static String hataKodu(RuntimeException e) {
        return e instanceof SinemaIstisnasi sinema ? sinema.getKod() : BEKLENMEYEN_HATA_KODU;
    }
}
