package com.sinema.catalog.duzen.service;

import com.sinema.catalog.duzen.dto.DuzenCevabi;
import com.sinema.catalog.duzen.dto.DuzenIstegi;
import com.sinema.catalog.duzen.dto.DuzenOzeti;
import com.sinema.catalog.duzen.dto.HucreIstegi;
import com.sinema.catalog.duzen.entity.DuzenDurumu;
import com.sinema.catalog.duzen.entity.DuzenHucresi;
import com.sinema.catalog.duzen.entity.SalonDuzeni;
import com.sinema.catalog.duzen.mapper.DuzenMapper;
import com.sinema.catalog.duzen.repository.DuzenHucresiRepository;
import com.sinema.catalog.duzen.repository.DuzenHucresiRepository.KoltukSayisi;
import com.sinema.catalog.duzen.repository.SalonDuzeniRepository;
import com.sinema.catalog.seans.repository.SeansRepository;
import com.sinema.catalog.sube.entity.Salon;
import com.sinema.catalog.sube.repository.SalonRepository;
import com.sinema.catalog.sube.service.SubeServisi;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.IsKuraliIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Düzen taslağı: oluşturma, güncelleme, listeleme. Yazma işlemleri salon satırını kilitler;
 * böylece aynı salonda aynı anda iki taslak açılamaz ve taslak, yayımlanırken değiştirilemez.
 */
@Service
@RequiredArgsConstructor
public class DuzenServisi {

    public static final String DUZEN_YOK_KODU = "LAYOUT_NOT_FOUND";
    public static final String TASLAK_VAR_KODU = "DRAFT_ALREADY_EXISTS";
    public static final String DUZENLENEMEZ_KODU = "LAYOUT_NOT_EDITABLE";
    public static final String YAYIMLANMIS_KODU = "LAYOUT_ALREADY_PUBLISHED";
    public static final String SUREN_SEANS_KODU = "HALL_SHOWTIME_IN_PROGRESS";
    public static final String GECERSIZ_DUZEN_KODU = "LAYOUT_INVALID";
    public static final String IHLALLER_ALANI = "violations";
    private static final Comparator<DuzenHucresi> KONUM_SIRASI =
            Comparator.comparingInt(DuzenHucresi::getSatir).thenComparingInt(DuzenHucresi::getSutun);

    private final SalonRepository salonRepository;
    private final SalonDuzeniRepository duzenRepository;
    private final DuzenHucresiRepository hucreRepository;
    private final DuzenDogrulayici dogrulayici;
    private final DuzenMapper duzenMapper;
    private final SeansRepository seansRepository;
    private final Clock saat;

    @Transactional
    public DuzenCevabi taslakOlustur(Long salonId, DuzenIstegi istek) {
        Salon salon = salonuKilitle(salonId);
        duzenRepository.findBySalonIdAndDurum(salonId, DuzenDurumu.DRAFT).ifPresent(taslak -> {
            throw new CakismaIstisnasi(TASLAK_VAR_KODU,
                    "Bu salonun zaten bir taslak düzeni var; onu güncelleyin.", Map.of("layoutId", taslak.getId()));
        });
        gecerliOldugunuDogrula(istek);
        SalonDuzeni duzen = duzenRepository.saveAndFlush(
                SalonDuzeni.taslakOlustur(salonId, istek.gridRows(), istek.gridCols()));
        return cevapOlustur(duzen, hucreleriKaydet(duzen.getId(), istek.cells()), salon);
    }

    @Transactional
    public DuzenCevabi taslakGuncelle(Long salonId, Long duzenId, DuzenIstegi istek) {
        Salon salon = salonuKilitle(salonId);
        SalonDuzeni duzen = duzenBul(salonId, duzenId);
        if (!duzen.taslakMi()) {
            throw new CakismaIstisnasi(DUZENLENEMEZ_KODU,
                    "Yayımlanmış düzen değiştirilemez; yeni bir taslak oluşturun.");
        }
        gecerliOldugunuDogrula(istek);
        // Sıra önemli: boyut değişikliği, hücre silme sorgusunun otomatik flush'ıyla birlikte yazılır
        duzen.boyutuGuncelle(istek.gridRows(), istek.gridCols());
        hucreRepository.duzeninHucreleriniSil(duzenId);
        return cevapOlustur(duzenBul(salonId, duzenId), hucreleriKaydet(duzenId, istek.cells()), salon);
    }

    /**
     * Taslağı bir sonraki sürüm numarasıyla yayımlar ve salonun aktif düzeni yapar. Yeni seanslar bu sürümü
     * kullanır, mevcut seanslar eski sürümde kalır (K17). K13: salonda süren bir seans varken yayımlanmaz
     * (değişiklik seanssız aralıkta yapılır). Mevcut biletlerin yeni düzene göçü M8'de (K35).
     */
    @Transactional
    public DuzenCevabi yayimla(Long salonId, Long duzenId) {
        Salon salon = salonuKilitle(salonId);
        SalonDuzeni duzen = duzenBul(salonId, duzenId);
        if (!duzen.taslakMi()) {
            throw new CakismaIstisnasi(YAYIMLANMIS_KODU, "Bu düzen zaten yayımlanmış.");
        }
        Instant simdi = saat.instant();
        if (seansRepository.surenSeansVarMi(salonId, simdi)) {
            throw new CakismaIstisnasi(SUREN_SEANS_KODU,
                    "Salonda süren bir seans var; düzen seanssız aralıkta yayımlanabilir (K13).");
        }
        duzen.yayimla(duzenRepository.sonSurum(salonId) + 1, simdi);
        salon.aktifDuzeniDegistir(duzen.getId());
        duzenRepository.flush();
        return cevapOlustur(duzen, hucreRepository.findAllByDuzenIdOrderBySatirAscSutunAsc(duzenId), salon);
    }

    @Transactional(readOnly = true)
    public List<DuzenOzeti> listele(Long salonId) {
        Salon salon = salonRepository.findById(salonId).orElseThrow(SubeServisi::salonYok);
        List<SalonDuzeni> duzenler = duzenRepository.findAllBySalonIdOrderByIdDesc(salonId);
        Map<Long, Long> koltukSayilari = hucreRepository
                .koltukSayilari(duzenler.stream().map(SalonDuzeni::getId).toList()).stream()
                .collect(Collectors.toMap(KoltukSayisi::getDuzenId, KoltukSayisi::getKoltukSayisi));
        return duzenler.stream()
                .map(duzen -> duzenMapper.duzenOzeti(duzen, koltukSayilari.getOrDefault(duzen.getId(), 0L),
                        aktifMi(duzen, salon)))
                .toList();
    }

    @Transactional(readOnly = true)
    public DuzenCevabi getir(Long salonId, Long duzenId) {
        Salon salon = salonRepository.findById(salonId).orElseThrow(SubeServisi::salonYok);
        SalonDuzeni duzen = duzenBul(salonId, duzenId);
        return cevapOlustur(duzen, hucreRepository.findAllByDuzenIdOrderBySatirAscSutunAsc(duzenId), salon);
    }

    private Salon salonuKilitle(Long salonId) {
        return salonRepository.findKilitliById(salonId).orElseThrow(SubeServisi::salonYok);
    }

    private SalonDuzeni duzenBul(Long salonId, Long duzenId) {
        return duzenRepository.findByIdAndSalonId(duzenId, salonId)
                .orElseThrow(() -> new KaynakBulunamadiIstisnasi(DUZEN_YOK_KODU, "Düzen bulunamadı."));
    }

    private void gecerliOldugunuDogrula(DuzenIstegi istek) {
        List<String> ihlaller = dogrulayici.ihlalleriBul(istek);
        if (!ihlaller.isEmpty()) {
            throw new IsKuraliIstisnasi(GECERSIZ_DUZEN_KODU, "Düzen kurallara uymuyor.",
                    Map.of(IHLALLER_ALANI, ihlaller));
        }
    }

    /** Satır-sütun sırasıyla eklenir: koltuk id'leri salondaki okuma sırasıyla artar (ör. A-1 < A-2 < B-1). */
    private List<DuzenHucresi> hucreleriKaydet(Long duzenId, List<HucreIstegi> hucreler) {
        List<DuzenHucresi> varliklar = hucreler.stream()
                .map(hucre -> hucreOlustur(duzenId, hucre))
                .sorted(KONUM_SIRASI)
                .toList();
        return hucreRepository.saveAll(varliklar);
    }

    private static DuzenHucresi hucreOlustur(Long duzenId, HucreIstegi hucre) {
        return switch (hucre.type()) {
            case SEAT -> DuzenHucresi.koltuk(duzenId, hucre.row(), hucre.col(), hucre.seatRowLabel(),
                    hucre.seatNumber(), hucre.seatType());
            case ENTRANCE, EXIT, SCREEN, WALL -> DuzenHucresi.koltukDisi(duzenId, hucre.row(), hucre.col(), hucre.type());
        };
    }

    private DuzenCevabi cevapOlustur(SalonDuzeni duzen, List<DuzenHucresi> hucreler, Salon salon) {
        return duzenMapper.duzenCevabi(duzen, hucreler, aktifMi(duzen, salon));
    }

    private static boolean aktifMi(SalonDuzeni duzen, Salon salon) {
        return Objects.equals(salon.getAktifDuzenId(), duzen.getId());
    }
}
