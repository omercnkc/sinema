package com.sinema.catalog.seans.service;

import com.sinema.catalog.config.KatalogAyarlari;
import com.sinema.catalog.duzen.entity.DuzenHucresi;
import com.sinema.catalog.duzen.entity.HucreTipi;
import com.sinema.catalog.duzen.entity.SalonDuzeni;
import com.sinema.catalog.duzen.mapper.DuzenMapper;
import com.sinema.catalog.duzen.repository.DuzenHucresiRepository;
import com.sinema.catalog.duzen.repository.SalonDuzeniRepository;
import com.sinema.catalog.film.entity.Film;
import com.sinema.catalog.film.repository.FilmRepository;
import com.sinema.catalog.film.service.FilmServisi;
import com.sinema.catalog.seans.dto.IcSeansBilgisi;
import com.sinema.catalog.seans.dto.KoltukCozumlemeCevabi;
import com.sinema.catalog.seans.dto.SeansDetayi;
import com.sinema.catalog.seans.dto.SeansDuzeni;
import com.sinema.catalog.seans.dto.SeansGuncellemeIstegi;
import com.sinema.catalog.seans.dto.SeansOlusturmaIstegi;
import com.sinema.catalog.seans.dto.SeansOzeti;
import com.sinema.catalog.seans.entity.Seans;
import com.sinema.catalog.seans.mapper.SeansMapper;
import com.sinema.catalog.seans.repository.SeansRepository;
import com.sinema.catalog.sube.entity.Salon;
import com.sinema.catalog.sube.repository.SalonRepository;
import com.sinema.catalog.sube.service.SubeServisi;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.IsKuraliIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seans ekleme/güncelleme ve halka açık seans sorguları. Seans silinmez, iptal edilmez (K34). */
@Service
@RequiredArgsConstructor
public class SeansServisi {

    public static final String SEANS_YOK_KODU = "SHOWTIME_NOT_FOUND";
    public static final String GECMIS_SEANS_KODU = "SHOWTIME_IN_PAST";
    public static final String GIZLI_FILM_KODU = "MOVIE_HIDDEN";
    public static final String DUZENSIZ_SALON_KODU = "HALL_HAS_NO_LAYOUT";
    public static final String CAKISMA_KODU = "SHOWTIME_OVERLAP";

    private final SeansRepository seansRepository;
    private final FilmRepository filmRepository;
    private final SalonRepository salonRepository;
    private final SalonDuzeniRepository duzenRepository;
    private final DuzenHucresiRepository hucreRepository;
    private final SeansMapper seansMapper;
    private final DuzenMapper duzenMapper;
    private final KatalogAyarlari ayarlar;
    private final Clock saat;

    /**
     * Salon satırı kilitlenir: aynı salona aynı anda iki seans eklenirse ikincisi birincinin bitmesini bekler
     * ve çakışma sorgusunda onu görür (§2.4-8). Düzen yayımı da aynı kilidi aldığı için seans, yayım sırasında
     * yarım kalmış bir aktif düzene bağlanamaz.
     */
    @Transactional
    public SeansDetayi olustur(SeansOlusturmaIstegi istek) {
        Instant simdi = saat.instant();
        if (!istek.startsAt().isAfter(simdi)) {
            throw new IsKuraliIstisnasi(GECMIS_SEANS_KODU, "Geçmiş bir zamana seans eklenemez.");
        }
        Film film = filmRepository.findById(istek.movieId()).orElseThrow(FilmServisi::filmYok);
        if (!film.isGorunur()) {
            throw new IsKuraliIstisnasi(GIZLI_FILM_KODU, "Gizlenmiş filme yeni seans eklenemez (K40).");
        }
        Salon salon = salonRepository.findKilitliById(istek.hallId()).orElseThrow(SubeServisi::salonYok);
        if (salon.getAktifDuzenId() == null) {
            throw new IsKuraliIstisnasi(DUZENSIZ_SALON_KODU, "Salonun yayımlanmış bir düzeni yok.");
        }
        // Fiyat boşsa salonun o anki varsayılanı kopyalanır; salon fiyatı sonradan değişse de seans etkilenmez (K23)
        Seans seans = Seans.olustur(film, salon, istek.startsAt(), ayarlar.seansArasiPayi(),
                Objects.requireNonNullElse(istek.ticketPrice(), salon.getVarsayilanBiletFiyati()),
                ofsetVeyaVarsayilan(istek.salesCloseOffsetMinutes()));
        cakismadiginiDogrula(salon.getId(), seans);
        return seansMapper.seansDetayi(seansRepository.save(seans), simdi);
    }

    @Transactional
    public SeansDetayi guncelle(Long seansId, SeansGuncellemeIstegi istek) {
        Seans seans = seansBul(seansId);
        seans.satisBilgileriniGuncelle(istek.ticketPrice(), istek.salesCloseOffsetMinutes());
        return seansMapper.seansDetayi(seans, saat.instant());
    }

    /** Tarih iş saat diliminde yorumlanır (Europe/Istanbul): "bugün" Türkiye'deki bugündür. Boşsa bugün. */
    @Transactional(readOnly = true)
    public List<SeansOzeti> filminSeanslari(Long filmId, LocalDate tarih, Long subeId) {
        Film film = filmRepository.findById(filmId).orElseThrow(FilmServisi::filmYok);
        if (!film.isGorunur()) {
            throw FilmServisi.filmYok();
        }
        Instant simdi = saat.instant();
        LocalDate gun = tarih != null ? tarih : LocalDate.ofInstant(simdi, ayarlar.isSaatDilimi());
        Instant gunBasi = gun.atStartOfDay(ayarlar.isSaatDilimi()).toInstant();
        Instant gunSonu = gun.plusDays(1).atStartOfDay(ayarlar.isSaatDilimi()).toInstant();
        return seansRepository.filminGunlukSeanslari(filmId, gunBasi, gunSonu, subeId).stream()
                .map(seans -> seansMapper.seansOzeti(seans, simdi))
                .toList();
    }

    /** Gizlenmiş filmin mevcut seansları erişilebilir kalır (K40: satılmış biletler etkilenmez). */
    @Transactional(readOnly = true)
    public SeansDetayi detay(Long seansId) {
        return seansMapper.seansDetayi(seansBul(seansId), saat.instant());
    }

    @Transactional(readOnly = true)
    public SeansDuzeni duzen(Long seansId) {
        Seans seans = seansRepository.findById(seansId).orElseThrow(SeansServisi::seansYok);
        SalonDuzeni duzen = duzenRepository.findById(seans.getDuzenId()).orElseThrow();
        return new SeansDuzeni(seans.getId(), duzen.getId(), duzen.getSurum(), duzen.getSatirSayisi(),
                duzen.getSutunSayisi(), hucreRepository.findAllByDuzenIdOrderBySatirAscSutunAsc(duzen.getId()).stream()
                        .map(duzenMapper::hucreCevabi)
                        .toList());
    }

    @Transactional(readOnly = true)
    public IcSeansBilgisi icBilgi(Long seansId) {
        return seansMapper.icSeansBilgisi(seansBul(seansId), saat.instant());
    }

    /**
     * İstenen hücreleri seansın mühürlü düzeninde arar. Başka düzenin hücresi (ör. salonun yeni sürümü)
     * bu seans için geçersizdir; koltuk kimliği yalnızca kendi düzeninde anlamlıdır (K17).
     */
    @Transactional(readOnly = true)
    public KoltukCozumlemeCevabi koltuklariCoz(Long seansId, List<Long> hucreIdleri) {
        Seans seans = seansRepository.findById(seansId).orElseThrow(SeansServisi::seansYok);
        Set<Long> istenenler = new LinkedHashSet<>(hucreIdleri);
        Map<Long, DuzenHucresi> koltuklar = hucreRepository.findAllByDuzenIdAndIdIn(seans.getDuzenId(), istenenler)
                .stream()
                .filter(hucre -> hucre.getTip() == HucreTipi.SEAT)
                .collect(Collectors.toMap(DuzenHucresi::getId, Function.identity()));
        List<KoltukCozumlemeCevabi.Koltuk> gecerli = istenenler.stream()
                .filter(koltuklar::containsKey)
                .map(id -> seansMapper.koltuk(koltuklar.get(id)))
                .toList();
        List<Long> gecersiz = istenenler.stream().filter(id -> !koltuklar.containsKey(id)).toList();
        return new KoltukCozumlemeCevabi(seans.getDuzenId(), gecerli, gecersiz);
    }

    public static KaynakBulunamadiIstisnasi seansYok() {
        return new KaynakBulunamadiIstisnasi(SEANS_YOK_KODU, "Seans bulunamadı.");
    }

    private Seans seansBul(Long seansId) {
        return seansRepository.detayIleBul(seansId).orElseThrow(SeansServisi::seansYok);
    }

    private void cakismadiginiDogrula(Long salonId, Seans yeni) {
        List<Long> cakisanlar = seansRepository.cakisanSeansIdleri(salonId, yeni.getBaslangic(), yeni.getBitis(),
                Limit.of(1));
        if (!cakisanlar.isEmpty()) {
            throw new CakismaIstisnasi(CAKISMA_KODU,
                    "Bu salonda aynı zaman aralığında başka bir seans var (seans arası payı dahil).",
                    Map.of("conflictingShowtimeId", cakisanlar.getFirst()));
        }
    }

    private int ofsetVeyaVarsayilan(Integer ofsetDakika) {
        return ofsetDakika != null ? ofsetDakika : (int) ayarlar.varsayilanSatisKapanisOfseti().toMinutes();
    }
}
