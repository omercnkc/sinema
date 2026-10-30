package com.sinema.catalog.film.service;

import com.sinema.catalog.film.dto.FilmDetayi;
import com.sinema.catalog.film.dto.FilmIstegi;
import com.sinema.catalog.film.dto.FilmOzeti;
import com.sinema.catalog.film.entity.Film;
import com.sinema.catalog.film.entity.FilmBilgileri;
import com.sinema.catalog.film.mapper.FilmMapper;
import com.sinema.catalog.film.repository.FilmRepository;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.IsKuraliIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import com.sinema.common.sayfa.SayfaCevabi;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Film yönetimi ve listeleme. Film silinmez, gizlenir (K40); gizli film halka açık uçlarda yok sayılır. */
@Service
@RequiredArgsConstructor
public class FilmServisi {

    public static final String FILM_YOK_KODU = "MOVIE_NOT_FOUND";
    public static final String FILM_VAR_KODU = "MOVIE_ALREADY_EXISTS";
    public static final String TMDB_ID_DEGISEMEZ_KODU = "TMDB_ID_IMMUTABLE";
    private static final String FILM_VAR_MESAJI = "Bu TMDB filmi zaten ekli.";

    // Yeni vizyon önce; vizyon tarihi boş olanlar (MySQL'de DESC sıralamada) sona düşer
    private static final Sort LISTE_SIRASI = Sort.by(Sort.Order.desc("vizyonTarihi"), Sort.Order.desc("id"));

    private final FilmRepository filmRepository;
    private final FilmMapper filmMapper;
    private final Clock saat;

    @Transactional
    public FilmDetayi olustur(FilmIstegi istek) {
        FilmBilgileri bilgiler = filmMapper.filmBilgileri(istek);
        if (istek.tmdbId() == null) {
            return filmMapper.filmDetayi(kaydetVeyaCakismaBildir(Film.elleOlustur(bilgiler)));
        }
        filmRepository.findByTmdbId(istek.tmdbId()).ifPresent(mevcut -> {
            throw filmZatenVar(mevcut.getId());
        });
        Film film = Film.tmdbdenOlustur(istek.tmdbId(), bilgiler, saat.instant());
        return filmMapper.filmDetayi(kaydetVeyaCakismaBildir(film));
    }

    @Transactional
    public FilmDetayi guncelle(Long filmId, FilmIstegi istek) {
        Film film = filmBul(filmId);
        if (istek.tmdbId() != null && !Objects.equals(istek.tmdbId(), film.getTmdbId())) {
            throw new IsKuraliIstisnasi(TMDB_ID_DEGISEMEZ_KODU,
                    "Filmin TMDB bağlantısı değiştirilemez; farklı bir film için yeni kayıt açın.");
        }
        film.bilgileriGuncelle(filmMapper.filmBilgileri(istek));
        return filmMapper.filmDetayi(film);
    }

    @Transactional
    public FilmDetayi gorunurlukDegistir(Long filmId, boolean gorunur) {
        Film film = filmBul(filmId);
        film.gorunurlukDegistir(gorunur);
        return filmMapper.filmDetayi(film);
    }

    @Transactional(readOnly = true)
    public SayfaCevabi<FilmOzeti> herkeseAcikAra(String arama, String tur, int sayfa, int boyut) {
        return ara(true, arama, tur, sayfa, boyut);
    }

    @Transactional(readOnly = true)
    public SayfaCevabi<FilmOzeti> yonetimIcinAra(String arama, int sayfa, int boyut) {
        return ara(false, arama, null, sayfa, boyut);
    }

    /** Gizli film halka açık detayda "yok" sayılır; varlığı sızdırılmaz. */
    @Transactional(readOnly = true)
    public FilmDetayi herkeseAcikGetir(Long filmId) {
        Film film = filmBul(filmId);
        if (!film.isGorunur()) {
            throw filmYok();
        }
        return filmMapper.filmDetayi(film);
    }

    @Transactional(readOnly = true)
    public FilmDetayi yonetimIcinGetir(Long filmId) {
        return filmMapper.filmDetayi(filmBul(filmId));
    }

    private SayfaCevabi<FilmOzeti> ara(boolean sadeceGorunur, String arama, String tur, int sayfa, int boyut) {
        Pageable sayfalama = PageRequest.of(sayfa, boyut, LISTE_SIRASI);
        return SayfaCevabi.from(filmRepository.ara(sadeceGorunur, bosIseNull(arama), bosIseNull(tur), sayfalama)
                .map(filmMapper::filmOzeti));
    }

    private Film filmBul(Long filmId) {
        return filmRepository.findById(filmId).orElseThrow(FilmServisi::filmYok);
    }

    /**
     * Aynı TMDB filmini aynı anda iki admin eklerse asıl güvence UNIQUE(tmdb_id). Kısıt hatasından sonra
     * Hibernate oturumu güvenilir değildir; mevcut filmin id'si bu yüzden yeniden sorgulanmaz.
     */
    private Film kaydetVeyaCakismaBildir(Film film) {
        try {
            return filmRepository.saveAndFlush(film);
        } catch (DataIntegrityViolationException e) {
            throw new CakismaIstisnasi(FILM_VAR_KODU, FILM_VAR_MESAJI);
        }
    }

    private static CakismaIstisnasi filmZatenVar(Long mevcutFilmId) {
        return new CakismaIstisnasi(FILM_VAR_KODU, FILM_VAR_MESAJI, Map.of("movieId", mevcutFilmId));
    }

    public static KaynakBulunamadiIstisnasi filmYok() {
        return new KaynakBulunamadiIstisnasi(FILM_YOK_KODU, "Film bulunamadı.");
    }

    private static String bosIseNull(String deger) {
        return StringUtils.hasText(deger) ? deger.trim() : null;
    }
}
