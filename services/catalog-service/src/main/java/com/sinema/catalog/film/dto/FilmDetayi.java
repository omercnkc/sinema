package com.sinema.catalog.film.dto;

import com.sinema.catalog.film.entity.AkilliIsaret;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Filmin tüm alanları (K16). tmdbId doluysa istemci TMDB atfını ve "TMDB puanı" etiketini gösterir.
 * cast başrol önce sıralıdır.
 */
public record FilmDetayi(
        Long id,
        Long tmdbId,
        String title,
        String overview,
        String posterPath,
        int runtimeMinutes,
        LocalDate releaseDate,
        String certification,
        BigDecimal voteAverage,
        String director,
        Set<String> genres,
        List<String> cast,
        Set<AkilliIsaret> contentDescriptors,
        boolean visible,
        Instant tmdbSyncedAt) {
}
