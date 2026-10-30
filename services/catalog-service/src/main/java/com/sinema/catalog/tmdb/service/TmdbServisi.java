package com.sinema.catalog.tmdb.service;

import com.sinema.catalog.film.entity.Film;
import com.sinema.catalog.film.repository.FilmRepository;
import com.sinema.catalog.tmdb.dto.TmdbAramaSonucu;
import com.sinema.catalog.tmdb.dto.TmdbFilmOnizlemesi;
import com.sinema.catalog.tmdb.istemci.TmdbAramaCevabi;
import com.sinema.catalog.tmdb.istemci.TmdbIstemcisi;
import com.sinema.catalog.tmdb.mapper.TmdbMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Admin'in TMDB'de arama ve form ön doldurma akışı (K18). Hiçbir şey kaydetmez: admin formu düzenleyip
 * POST /movies ile kaydeder. TMDB çağrısı DB transaction'ı dışında yapılır (yavaş dış çağrı bağlantı tutmasın).
 */
@Service
@RequiredArgsConstructor
public class TmdbServisi {

    private final TmdbIstemcisi tmdbIstemcisi;
    private final TmdbDonusturucu donusturucu;
    private final TmdbMapper tmdbMapper;
    private final FilmRepository filmRepository;

    public TmdbAramaSonucu ara(String sorgu, int sayfa) {
        TmdbAramaCevabi cevap = tmdbIstemcisi.ara(sorgu.trim(), sayfa);
        List<TmdbAramaCevabi.Sonuc> sonuclar = cevap.results() == null ? List.of() : cevap.results();
        Map<Long, Long> ekliFilmler = filmRepository
                .findAllByTmdbIdIn(sonuclar.stream().map(TmdbAramaCevabi.Sonuc::id).toList()).stream()
                .collect(Collectors.toMap(Film::getTmdbId, Film::getId));
        List<TmdbAramaSonucu.Aday> adaylar = sonuclar.stream()
                .map(sonuc -> {
                    TmdbAramaSonucu.Aday aday = tmdbMapper.aday(sonuc);
                    return new TmdbAramaSonucu.Aday(aday.tmdbId(), aday.title(), aday.originalTitle(),
                            aday.releaseDate(), aday.posterPath(), ekliFilmler.get(aday.tmdbId()));
                })
                .toList();
        return new TmdbAramaSonucu(cevap.page(), cevap.totalPages(), cevap.totalResults(), adaylar);
    }

    public TmdbFilmOnizlemesi onizleme(long tmdbId) {
        TmdbFilmOnizlemesi onizleme = donusturucu.onizleme(tmdbIstemcisi.filmGetir(tmdbId));
        return onizleme.eklenmisFilmIle(filmRepository.findByTmdbId(tmdbId).map(Film::getId).orElse(null));
    }
}
