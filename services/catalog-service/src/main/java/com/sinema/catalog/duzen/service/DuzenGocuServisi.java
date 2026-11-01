package com.sinema.catalog.duzen.service;

import com.sinema.catalog.duzen.dto.GocKaynagi;
import com.sinema.catalog.duzen.dto.GocKaynagi.DuzenHucreleri;
import com.sinema.catalog.duzen.dto.GocKaynagi.GocSeansi;
import com.sinema.catalog.duzen.entity.SalonDuzeni;
import com.sinema.catalog.duzen.mapper.DuzenMapper;
import com.sinema.catalog.duzen.repository.DuzenHucresiRepository;
import com.sinema.catalog.duzen.repository.SalonDuzeniRepository;
import com.sinema.catalog.seans.entity.Seans;
import com.sinema.catalog.seans.repository.SeansRepository;
import com.sinema.catalog.sube.repository.SalonRepository;
import com.sinema.catalog.sube.service.SubeServisi;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bilet göçünün catalog tarafındaki okuma kısmı (K35): booking planı bu veriden kendisi hesaplar, catalog
 * biletleri bilmez (yön booking → catalog, §3.3). Yazma kısmı `DuzenServisi.etkinlestir`'de.
 */
@Service
@RequiredArgsConstructor
public class DuzenGocuServisi {

    private final SalonRepository salonRepository;
    private final SalonDuzeniRepository duzenRepository;
    private final DuzenHucresiRepository hucreRepository;
    private final SeansRepository seansRepository;
    private final DuzenMapper duzenMapper;
    private final Clock saat;

    /** Hedef, salonun taslağı olmalı; seanslar başlamamış olanlar (başlamış seanslar eski sürümde kalır, K51). */
    @Transactional(readOnly = true)
    public GocKaynagi gocKaynagi(Long salonId, Long hedefDuzenId) {
        if (!salonRepository.existsById(salonId)) {
            throw SubeServisi.salonYok();
        }
        SalonDuzeni hedef = duzenRepository.findByIdAndSalonId(hedefDuzenId, salonId)
                .orElseThrow(() -> new KaynakBulunamadiIstisnasi(DuzenServisi.DUZEN_YOK_KODU, "Düzen bulunamadı."));
        if (!hedef.taslakMi()) {
            throw new CakismaIstisnasi(DuzenServisi.YAYIMLANMIS_KODU, "Bu düzen zaten yayımlanmış.");
        }
        List<Seans> seanslar = seansRepository.baslamamisSeanslar(salonId, saat.instant());
        List<DuzenHucreleri> kaynaklar = seanslar.stream()
                .map(Seans::getDuzenId)
                .distinct()
                .map(duzenId -> hucreler(duzenRepository.findById(duzenId).orElseThrow()))
                .toList();
        List<GocSeansi> seansOzetleri = seanslar.stream()
                .map(seans -> new GocSeansi(seans.getId(), seans.getBaslangic(), seans.getDuzenId()))
                .toList();
        return new GocKaynagi(salonId, hucreler(hedef), seansOzetleri, kaynaklar);
    }

    private DuzenHucreleri hucreler(SalonDuzeni duzen) {
        return new DuzenHucreleri(duzen.getId(), duzen.getSatirSayisi(), duzen.getSutunSayisi(),
                hucreRepository.findAllByDuzenIdOrderBySatirAscSutunAsc(duzen.getId()).stream()
                        .map(duzenMapper::hucreCevabi)
                        .toList());
    }
}
