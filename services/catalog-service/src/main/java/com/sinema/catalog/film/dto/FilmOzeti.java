package com.sinema.catalog.film.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/** Film listesindeki kart (planlama §7.4): afiş, tür, süre, yaş sınırı, TMDB puanı. */
public record FilmOzeti(
        Long id,
        String title,
        String posterPath,
        int runtimeMinutes,
        LocalDate releaseDate,
        String certification,
        BigDecimal voteAverage,
        Set<String> genres,
        boolean visible) {
}
