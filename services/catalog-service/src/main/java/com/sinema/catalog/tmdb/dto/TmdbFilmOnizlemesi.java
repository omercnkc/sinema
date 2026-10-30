package com.sinema.catalog.tmdb.dto;

import com.sinema.catalog.film.entity.AkilliIsaret;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Admin film formunu ön dolduran veri (K18). Alan adları FilmIstegi ile aynıdır: istemci bunu düzenleyip
 * olduğu gibi POST /movies'e gönderebilir. Kaydedilmez. movieId doluysa bu TMDB filmi zaten eklidir.
 * TMDB'de olmayan alanlar null gelir (ör. TR yaş sınırı yoksa certification), admin elle doldurur.
 */
public record TmdbFilmOnizlemesi(
        long tmdbId,
        String title,
        String overview,
        String posterPath,
        Integer runtimeMinutes,
        LocalDate releaseDate,
        String certification,
        BigDecimal voteAverage,
        String director,
        Set<String> genres,
        List<String> cast,
        Set<AkilliIsaret> contentDescriptors,
        Long movieId) {

    public TmdbFilmOnizlemesi eklenmisFilmIle(Long mevcutFilmId) {
        return new TmdbFilmOnizlemesi(tmdbId, title, overview, posterPath, runtimeMinutes, releaseDate, certification,
                voteAverage, director, genres, cast, contentDescriptors, mevcutFilmId);
    }
}
