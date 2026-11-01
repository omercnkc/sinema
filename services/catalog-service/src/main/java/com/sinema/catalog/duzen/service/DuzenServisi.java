package com.sinema.catalog.duzen.service;

import com.sinema.catalog.duzen.dto.DuzenCevabi;
import com.sinema.catalog.duzen.dto.DuzenEtkinlestirmeCevabi;
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
import com.sinema.catalog.seans.entity.Seans;
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
import java.util.Set;
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
    public static final String GOC_GEREKLI_KODU = "LAYOUT_MIGRATION_REQUIRED";
    public static final String GOC_SEANSLARI_DEGISTI_KODU = "MIGRATION_SHOWTIMES_CHANGED";
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
     * Taslağı bir sonraki sürüm numarasıyla yayımlar ve salonun aktif düzeni yapar; yalnızca başlamamış seansı
     * olmayan salonda (K51). Başlamamış seans varsa biletleri olabileceği için yayım booking'in göç akışıyla
     * yapılır (`etkinlestir`, K35). K13: salonda süren bir seans varken yayımlanmaz.
     */
    @Transactional
    public DuzenCevabi yayimla(Long salonId, Long duzenId) {
        Salon salon = salonuKilitle(salonId);
        SalonDuzeni duzen = duzenBul(salonId, duzenId);
        if (!duzen.taslakMi()) {
            throw new CakismaIstisnasi(YAYIMLANMIS_KODU, "Bu düzen zaten yayımlanmış.");
        }
        Instant simdi = saat.instant();
        surenSeansYoksaDevamEt(salonId, simdi);
        if (seansRepository.baslamamisSeansVarMi(salonId, simdi)) {
            throw new CakismaIstisnasi(GOC_GEREKLI_KODU,
                    "Salonun başlamamış seansları var; düzen bilet göçüyle yayımlanır (K35).");
        }
        yayimlaVeAktifYap(salon, duzen, simdi);
        return cevapOlustur(duzen, hucreRepository.findAllByDuzenIdOrderBySatirAscSutunAsc(duzenId), salon);
    }

    /**
     * Göçün son adımı (booking çağırır, §7.8): taslağı yayımlar, aktif düzen yapar ve salonun başlamamış bütün
     * seanslarını bu sürüme taşır. `seansIdleri` booking'in biletlerini taşıdığı seanslardır; biri bu arada
     * başlamışsa ya da salonda değilse 409 (booking göçü geri alır). Listede olmayan başlamamış seanslar da
     * taşınır: göç boyunca salonda koltuk tutulamadığı için biletleri yoktur. Aynı düzenle tekrar çağrı (ör.
     * booking önceki cevabı alamadı) hata vermez, sonucu yeniden uygular.
     */
    @Transactional
    public DuzenEtkinlestirmeCevabi etkinlestir(Long salonId, Long duzenId, List<Long> seansIdleri) {
        Salon salon = salonuKilitle(salonId);
        SalonDuzeni duzen = duzenBul(salonId, duzenId);
        Instant simdi = saat.instant();
        if (duzen.taslakMi()) {
            surenSeansYoksaDevamEt(salonId, simdi);
            seanslarinBaslamadiginiDogrula(salonId, seansIdleri, simdi);
            yayimlaVeAktifYap(salon, duzen, simdi);
        } else if (!aktifMi(duzen, salon)) {
            throw new CakismaIstisnasi(YAYIMLANMIS_KODU, "Bu düzen zaten yayımlanmış ve salonun aktif düzeni değil.");
        }
        int tasinan = seansRepository.baslamamisSeanslariTasi(salonId, duzenId, simdi);
        return new DuzenEtkinlestirmeCevabi(duzenId, duzen.getSurum(), tasinan);
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

    private void surenSeansYoksaDevamEt(Long salonId, Instant simdi) {
        if (seansRepository.surenSeansVarMi(salonId, simdi)) {
            throw new CakismaIstisnasi(SUREN_SEANS_KODU,
                    "Salonda süren bir seans var; düzen seanssız aralıkta yayımlanabilir (K13).");
        }
    }

    private void seanslarinBaslamadiginiDogrula(Long salonId, List<Long> seansIdleri, Instant simdi) {
        Set<Long> uygunlar = seansRepository.findAllById(seansIdleri).stream()
                .filter(seans -> seans.getSalon().getId().equals(salonId) && seans.getBaslangic().isAfter(simdi))
                .map(Seans::getId)
                .collect(Collectors.toSet());
        List<Long> uygunOlmayanlar = seansIdleri.stream().filter(id -> !uygunlar.contains(id)).toList();
        if (!uygunOlmayanlar.isEmpty()) {
            throw new CakismaIstisnasi(GOC_SEANSLARI_DEGISTI_KODU,
                    "Göç planındaki bazı seanslar başladı ya da bu salonda değil; göç yeniden hesaplanmalı.",
                    Map.of("showtimeIds", uygunOlmayanlar));
        }
    }

    /** Yayımın çekirdeği (admin yayımı ve göç ortak): sürüm numarası, yayım anı, salonun aktif düzeni. */
    private void yayimlaVeAktifYap(Salon salon, SalonDuzeni duzen, Instant simdi) {
        duzen.yayimla(duzenRepository.sonSurum(salon.getId()) + 1, simdi);
        salon.aktifDuzeniDegistir(duzen.getId());
        duzenRepository.flush();
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
