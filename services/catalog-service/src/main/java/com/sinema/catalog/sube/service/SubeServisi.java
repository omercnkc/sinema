package com.sinema.catalog.sube.service;

import com.sinema.catalog.sube.dto.SalonCevabi;
import com.sinema.catalog.sube.dto.SalonGuncellemeIstegi;
import com.sinema.catalog.sube.dto.SalonOlusturmaIstegi;
import com.sinema.catalog.sube.dto.SubeCevabi;
import com.sinema.catalog.sube.dto.SubeIstegi;
import com.sinema.catalog.sube.dto.YonetimSubeCevabi;
import com.sinema.catalog.sube.entity.Salon;
import com.sinema.catalog.sube.entity.Sube;
import com.sinema.catalog.sube.mapper.SubeMapper;
import com.sinema.catalog.sube.repository.SalonRepository;
import com.sinema.catalog.sube.repository.SubeRepository;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Şube ve salon yönetimi. Kayıtlar silinmez (db-planlama §5): yalnızca oluşturma ve güncelleme var. */
@Service
@RequiredArgsConstructor
public class SubeServisi {

    public static final String SUBE_YOK_KODU = "CINEMA_NOT_FOUND";
    public static final String SALON_YOK_KODU = "HALL_NOT_FOUND";
    public static final String SALON_ADI_ALINMIS_KODU = "HALL_NAME_TAKEN";
    private static final String SALON_ADI_ALINMIS_MESAJI = "Bu şubede aynı adlı bir salon var.";

    private final SubeRepository subeRepository;
    private final SalonRepository salonRepository;
    private final SubeMapper subeMapper;

    @Transactional(readOnly = true)
    public List<SubeCevabi> listele() {
        return subeMapper.subeCevaplari(subeRepository.findAllByOrderByIdAsc());
    }

    @Transactional(readOnly = true)
    public List<YonetimSubeCevabi> yonetimIcinListele() {
        return subeMapper.yonetimSubeCevaplari(subeRepository.findAllByOrderByIdAsc());
    }

    @Transactional
    public YonetimSubeCevabi subeOlustur(SubeIstegi istek) {
        Sube sube = subeRepository.save(Sube.olustur(istek.name(), istek.city(), istek.address()));
        return subeMapper.yonetimSubeCevabi(sube);
    }

    @Transactional
    public YonetimSubeCevabi subeGuncelle(Long subeId, SubeIstegi istek) {
        Sube sube = subeBul(subeId);
        sube.bilgileriGuncelle(istek.name(), istek.city(), istek.address());
        return subeMapper.yonetimSubeCevabi(sube);
    }

    @Transactional
    public SalonCevabi salonOlustur(SalonOlusturmaIstegi istek) {
        Sube sube = subeBul(istek.cinemaId());
        String ad = istek.name().trim();
        if (salonRepository.existsBySubeIdAndAd(sube.getId(), ad)) {
            throw salonAdiAlinmis();
        }
        Salon salon = Salon.olustur(sube, ad, istek.defaultTicketPrice());
        return subeMapper.salonCevabi(kaydetVeyaCakismaBildir(salon));
    }

    @Transactional
    public SalonCevabi salonGuncelle(Long salonId, SalonGuncellemeIstegi istek) {
        Salon salon = salonRepository.findById(salonId).orElseThrow(SubeServisi::salonYok);
        String ad = istek.name().trim();
        if (salonRepository.existsBySubeIdAndAdAndIdNot(salon.getSube().getId(), ad, salonId)) {
            throw salonAdiAlinmis();
        }
        salon.bilgileriGuncelle(ad, istek.defaultTicketPrice());
        return subeMapper.salonCevabi(kaydetVeyaCakismaBildir(salon));
    }

    /** Başka servisler (düzen, seans) salonu bulamazsa aynı 404'ü versin diye paylaşılır. */
    public static KaynakBulunamadiIstisnasi salonYok() {
        return new KaynakBulunamadiIstisnasi(SALON_YOK_KODU, "Salon bulunamadı.");
    }

    private Sube subeBul(Long subeId) {
        return subeRepository.findById(subeId)
                .orElseThrow(() -> new KaynakBulunamadiIstisnasi(SUBE_YOK_KODU, "Şube bulunamadı."));
    }

    /** Ön kontrolü aynı anda geçen iki istek olursa asıl güvence UNIQUE(cinema_id, name) kısıtıdır. */
    private Salon kaydetVeyaCakismaBildir(Salon salon) {
        try {
            return salonRepository.saveAndFlush(salon);
        } catch (DataIntegrityViolationException e) {
            throw salonAdiAlinmis();
        }
    }

    private static CakismaIstisnasi salonAdiAlinmis() {
        return new CakismaIstisnasi(SALON_ADI_ALINMIS_KODU, SALON_ADI_ALINMIS_MESAJI);
    }
}
