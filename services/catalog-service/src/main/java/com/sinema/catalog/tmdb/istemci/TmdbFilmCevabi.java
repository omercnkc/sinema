package com.sinema.catalog.tmdb.istemci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * TMDB "GET /movie/{id}?append_to_response=release_dates,credits" cevabının kullandığımız kısmı.
 * Yalnızca TMDB istemcisi bilir; uygulamanın geri kalanı TmdbDonusturucu'nun çıktısıyla çalışır.
 * Bilinmeyen alanlar yok sayılır: TMDB yeni alan eklediğinde istemci bozulmasın.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbFilmCevabi(
        long id,
        String title,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        Integer runtime,
        // Boş string ("") gelebilir; LocalDate'e dönüşüm TmdbDonusturucu'da
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("vote_average") Double voteAverage,
        List<Ad> genres,
        Kadro credits,
        @JsonProperty("release_dates") VizyonKayitlari releaseDates) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ad(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Kadro(List<Oyuncu> cast, List<Ekip> crew) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Oyuncu(String name, Integer order) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ekip(String name, String job) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VizyonKayitlari(List<UlkeVizyonu> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UlkeVizyonu(@JsonProperty("iso_3166_1") String ulkeKodu,
            @JsonProperty("release_dates") List<Vizyon> vizyonlar) {
    }

    /** type: 1 premiere, 2 sınırlı sinema, 3 sinema, 4 dijital, 5 fiziksel, 6 TV. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Vizyon(String certification, List<String> descriptors, Integer type) {
    }
}
