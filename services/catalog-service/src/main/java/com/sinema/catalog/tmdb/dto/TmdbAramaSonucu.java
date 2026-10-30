package com.sinema.catalog.tmdb.dto;

import java.time.LocalDate;
import java.util.List;

/** Admin'in TMDB araması; movieId doluysa film zaten ekli (admin aynı filmi ikinci kez eklemeye çalışmasın). */
public record TmdbAramaSonucu(int page, int totalPages, int totalResults, List<Aday> results) {

    public record Aday(long tmdbId, String title, String originalTitle, LocalDate releaseDate, String posterPath,
            Long movieId) {
    }
}
