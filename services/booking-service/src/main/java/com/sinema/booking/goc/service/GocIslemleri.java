package com.sinema.booking.goc.service;

import com.sinema.booking.bildirim.entity.Bildirim;
import com.sinema.booking.bildirim.repository.BildirimRepository;
import com.sinema.booking.doluluk.service.DolulukTemizleyici;
import com.sinema.booking.goc.entity.DuzenGocu;
import com.sinema.booking.goc.entity.KoltukGocKaydi;
import com.sinema.booking.goc.plan.KoltukTasima;
import com.sinema.booking.goc.plan.SeansGocPlani;
import com.sinema.booking.goc.repository.DuzenGocuRepository;
import com.sinema.booking.goc.repository.KoltukGocDeposu;
import com.sinema.booking.goc.repository.KoltukGocKaydiRepository;
import com.sinema.booking.katalog.istemci.KatalogGocKaynagi;
import com.sinema.booking.rezervasyon.entity.Rezervasyon;
import com.sinema.booking.rezervasyon.repository.RezervasyonRepository;
import com.sinema.common.hata.CakismaIstisnasi;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Göçün veritabanı adımları; her biri kendi transaction'ı (GocUygulamaServisi sırayla çağırır, catalog çağrıları
 * aralarında ve transaction dışında). Bildirimler taşıma anında değil, catalog etkinleştirdikten sonra
 * (tamamla) denetim kaydından yazılır: geri alınan göçün bildirimi hiç oluşmaz.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class GocIslemleri {

    static final String GOC_CAKISMASI_KODU = "MIGRATION_CONFLICT";

    private final DuzenGocuRepository gocRepository;
    private final KoltukGocKaydiRepository kayitRepository;
    private final KoltukGocDeposu gocDeposu;
    private final BildirimRepository bildirimRepository;
    private final RezervasyonRepository rezervasyonRepository;
    private final DolulukTemizleyici dolulukTemizleyici;

    /**
     * Göç kaydı ve salon kilidi birlikte; commit'ten sonra salonda yeni koltuk tutulamaz (plan bundan sonra
     * hesaplanır).
     *
     * @throws org.springframework.dao.DuplicateKeyException salonda süren bir göç var (kayıt da geri alınır)
     */
    @Transactional
    public DuzenGocu kilitleVeBasla(Long salonId, Long hedefDuzenId, Long kullaniciId, Instant simdi) {
        DuzenGocu gocu = gocRepository.saveAndFlush(DuzenGocu.basla(salonId, hedefDuzenId, kullaniciId, simdi));
        gocDeposu.kilitle(salonId, gocu.getId(), simdi);
        return gocu;
    }

    /** Hiçbir bilet değişmeden vazgeçildi (plan uygulanamaz, catalog okunamadı, taşıma geri alındı). */
    @Transactional
    public void vazgec(Long gocId, String hataKodu, Instant simdi) {
        DuzenGocu gocu = gocRepository.findById(gocId).orElseThrow();
        gocu.geriAlindi(hataKodu, simdi);
        gocDeposu.kilidiKaldir(gocu.getSalonId(), gocId);
    }

    /**
     * Biletleri yeni düzenin hücrelerine taşır ve denetim kaydını yazar (hepsi ya da hiçbiri). Seans ve eski hücre
     * sırasıyla işlenir (eşzamanlı onay/bırakma ile kilit sırası tutarlı). Bu arada boşalan koltuk atlanır. Sonunda
     * seanslarda yeni düzenin dışında dolu koltuk kalmadığı doğrulanır; kaldıysa (planlamadan sonra gelen yazma)
     * her şey geri alınır.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void biletleriTasi(Long gocId, GocHesabi hesap, Instant simdi) {
        for (SeansGocPlani plan : hesap.planlar()) {
            plan.tasimalar().stream()
                    .sorted(Comparator.comparingLong(KoltukTasima::eskiHucreId))
                    .forEach(tasima -> tasi(gocId, plan.seansId(), tasima, simdi));
        }
        Set<Long> yeniHucreler = hesap.kaynak().targetLayout().cells().stream()
                .map(KatalogGocKaynagi.Hucre::cellId).collect(Collectors.toSet());
        List<Long> seanslar = hesap.planlar().stream().map(SeansGocPlani::seansId).toList();
        if (gocDeposu.eskiHucredeKalanDoluKoltuklar(seanslar, yeniHucreler, simdi) > 0) {
            throw new CakismaIstisnasi(GOC_CAKISMASI_KODU,
                    "Göç hesaplanırken seanslarda koltuk değişti; göçü yeniden uygulayın.");
        }
    }

    /** catalog etkinleştirdi: bildirimler yazılır, göç tamamlanır, kilit kalkar. */
    @Transactional
    public void tamamla(Long gocId, Instant simdi) {
        DuzenGocu gocu = gocRepository.findById(gocId).orElseThrow();
        List<KoltukGocKaydi> kayitlar = kayitRepository.findAllByGocIdOrderById(gocId);
        bildirimleriYaz(kayitlar, simdi);
        gocu.tamamla(simdi);
        gocDeposu.kilidiKaldir(gocu.getSalonId(), gocId);
        doluluguTemizle(kayitlar);
    }

    /** catalog net bir iş hatasıyla reddetti (kendi tarafında hiçbir şey değişmedi): biletler eski yerine döner. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void geriAl(Long gocId, String hataKodu, Instant simdi) {
        DuzenGocu gocu = gocRepository.findById(gocId).orElseThrow();
        List<KoltukGocKaydi> kayitlar = kayitRepository.findAllByGocIdOrderById(gocId);
        for (KoltukGocKaydi kayit : kayitlar) {
            if (gocDeposu.koltuguTasi(kayit.getSeansId(), kayit.getRezervasyonId(), kayit.getYeniHucreId(),
                    kayit.getEskiHucreId(), simdi)) {
                gocDeposu.rezervasyonKoltugunuTasi(kayit.getRezervasyonId(), kayit.getYeniHucreId(),
                        kayit.getEskiHucreId(), kayit.getEskiEtiket());
            }
        }
        gocu.geriAlindi(hataKodu, simdi);
        gocDeposu.kilidiKaldir(gocu.getSalonId(), gocId);
        doluluguTemizle(kayitlar);
    }

    /** catalog'un durumu belirsiz: biletler taşınmış kalır, kilit kalır (koltuk tutulamaz), göç sürdürülebilir. */
    @Transactional
    public void askiyaAl(Long gocId, String hataKodu) {
        gocRepository.findById(gocId).orElseThrow().askiyaAlindi(hataKodu);
    }

    @Transactional(readOnly = true)
    public List<Long> tasinanSeanslar(Long gocId) {
        return kayitRepository.findAllByGocIdOrderById(gocId).stream().map(KoltukGocKaydi::getSeansId).distinct()
                .toList();
    }

    private void tasi(Long gocId, Long seansId, KoltukTasima tasima, Instant simdi) {
        String biletEtiketi = gocDeposu.biletEtiketi(tasima.rezervasyonId(), tasima.eskiHucreId());
        if (!gocDeposu.koltuguTasi(seansId, tasima.rezervasyonId(), tasima.eskiHucreId(), tasima.yeniHucreId(),
                simdi)) {
            log.info("Göç {}: seans {} koltuk {} bu arada boşaldı, taşınmadı", gocId, seansId, tasima.eskiHucreId());
            return;
        }
        if (gocDeposu.rezervasyonKoltugunuTasi(tasima.rezervasyonId(), tasima.eskiHucreId(), tasima.yeniHucreId(),
                tasima.yeniEtiket()) != 1) {
            throw new IllegalStateException("Rezervasyon " + tasima.rezervasyonId() + " koltuğu bulunamadı: "
                    + tasima.eskiHucreId());
        }
        kayitRepository.save(KoltukGocKaydi.olustur(gocId, seansId, tasima, biletEtiketi, simdi));
    }

    /** Rezervasyon başına tek bildirim; yalnızca etiketi değişen ya da yedek atanan koltuklar (K51). */
    private void bildirimleriYaz(List<KoltukGocKaydi> kayitlar, Instant simdi) {
        Map<Long, List<KoltukGocKaydi>> rezervasyonaGore = kayitlar.stream()
                .filter(kayit -> kayit.getNeden().bildirimGerekir())
                .collect(Collectors.groupingBy(KoltukGocKaydi::getRezervasyonId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, Rezervasyon> rezervasyonlar = rezervasyonRepository.findAllById(rezervasyonaGore.keySet()).stream()
                .collect(Collectors.toMap(Rezervasyon::getId, Function.identity()));
        rezervasyonaGore.forEach((rezervasyonId, degisiklikler) -> {
            Rezervasyon rezervasyon = rezervasyonlar.get(rezervasyonId);
            bildirimRepository.save(Bildirim.koltukDegisti(rezervasyon.getKullaniciId(), rezervasyonId,
                    KoltukDegisikligiMesaji.olustur(rezervasyon.getFilmAdi(), rezervasyon.getSubeAdi(),
                            rezervasyon.getSalonAdi(), degisiklikler), simdi));
        });
    }

    private void doluluguTemizle(List<KoltukGocKaydi> kayitlar) {
        kayitlar.stream().map(KoltukGocKaydi::getSeansId).distinct().forEach(dolulukTemizleyici::degisti);
    }
}
