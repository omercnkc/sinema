package com.sinema.catalog.tmdb.istemci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** TMDB "GET /search/movie" cevabının kullandığımız kısmı. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbAramaCevabi(
        int page,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("total_results") int totalResults,
        List<Sonuc> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sonuc(
            long id,
            String title,
            @JsonProperty("original_title") String originalTitle,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("poster_path") String posterPath) {
    }
}
