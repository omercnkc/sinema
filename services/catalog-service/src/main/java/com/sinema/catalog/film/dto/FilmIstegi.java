package com.sinema.catalog.film.dto;

import com.sinema.catalog.film.entity.AkilliIsaret;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Film oluşturma ve güncelleme. Admin formu TMDB'den doldurulmuş olabilir (tmdbId dolu) ya da elle
 * girilmiş olabilir (tmdbId boş). tmdbId oluşturulduktan sonra değiştirilemez.
 */
public record FilmIstegi(
        @Positive Long tmdbId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String overview,
        // TMDB görsel yolu, ör. "/yjMuqAyJUoQZGWsZ0vZuYj5inAR.jpg"; tam adres istemcide kurulur (K41 hotlink)
        @Size(max = 255) @Pattern(regexp = "/[A-Za-z0-9._-]+", message = "TMDB görsel yolu olmalı (ör. /abc.jpg)")
        String posterPath,
        @NotNull @Min(1) @Max(600) Integer runtimeMinutes,
        LocalDate releaseDate,
        @Pattern(regexp = TR_YAS_SINIRLARI, message = "Geçerli bir TR yaş sınırı olmalı") String certification,
        @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 1) BigDecimal voteAverage,
        @Size(max = 255) String director,
        @Size(max = 10) Set<@NotBlank @Size(max = 50) String> genres,
        @Size(max = 20) List<@NotBlank @Size(max = 255) String> cast,
        Set<@NotNull AkilliIsaret> contentDescriptors) {

    /** TMDB'nin TR sertifika listesi (developer.themoviedb.org, certification/movie/list). */
    public static final String TR_YAS_SINIRLARI = "Genel İzleyici Kitlesi|6A|6\\+|10A|10\\+|13A|13\\+|16\\+|18\\+";
}
