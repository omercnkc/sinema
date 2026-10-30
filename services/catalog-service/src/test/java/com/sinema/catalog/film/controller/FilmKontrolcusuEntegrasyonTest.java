package com.sinema.catalog.film.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import com.sinema.testdestegi.TestTokenUretici;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MysqlTestYapilandirmasi.class)
class FilmKontrolcusuEntegrasyonTest {

    private static final String ADMIN_TOKENI = "Bearer " + TestTokenUretici.kullaniciTokeni(1, "ADMIN");
    private static final String YONETIM = "/api/v1/admin/catalog/movies";
    private static final String KATALOG = "/api/v1/catalog/movies";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateTmdbMovieWithListsInOrder_whenAdminSendsPrefilledForm() throws Exception {
        long tmdbId = rastgeleTmdbId();

        yonetim(post(YONETIM), filmGovdesi("Dövüş Kulübü " + UUID.randomUUID(), tmdbId, "Dram"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tmdbId").value(tmdbId))
                .andExpect(jsonPath("$.tmdbSyncedAt").isNotEmpty())
                .andExpect(jsonPath("$.visible").value(true))
                .andExpect(jsonPath("$.certification").value("18+"))
                .andExpect(jsonPath("$.voteAverage").value(8.4))
                .andExpect(jsonPath("$.cast", contains("Brad Pitt", "Edward Norton", "Helena Bonham Carter")))
                .andExpect(jsonPath("$.contentDescriptors", containsInAnyOrder("SIDDET_KORKU", "CINSELLIK")));
    }

    @Test
    void shouldCreateManualMovieWithoutSyncTime_whenTmdbIdMissing() throws Exception {
        yonetim(post(YONETIM), filmGovdesi("Yerli Film " + UUID.randomUUID(), null, "Komedi"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tmdbId").isEmpty())
                .andExpect(jsonPath("$.tmdbSyncedAt").isEmpty());
    }

    @Test
    void shouldReturnConflictWithExistingId_whenTmdbMovieAddedTwice() throws Exception {
        long tmdbId = rastgeleTmdbId();
        long filmId = filmOlustur("Başlangıç " + UUID.randomUUID(), tmdbId, "Bilim Kurgu");

        yonetim(post(YONETIM), filmGovdesi("Başlangıç tekrar", tmdbId, "Bilim Kurgu"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MOVIE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.movieId").value(filmId));
    }

    @Test
    void shouldHideMovieFromPublicButNotFromAdmin_whenVisibilityTurnedOff() throws Exception {
        String tur = "Tür-" + UUID.randomUUID();
        long filmId = filmOlustur("Gizlenecek Film", null, tur);

        yonetim(patch(YONETIM + "/" + filmId + "/visibility"), "{\"visible\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible").value(false));

        mockMvc.perform(get(KATALOG + "/" + filmId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MOVIE_NOT_FOUND"));
        mockMvc.perform(get(KATALOG).param("genre", tur))
                .andExpect(jsonPath("$.totalElements").value(0));
        yonetim(get(YONETIM + "/" + filmId), null).andExpect(status().isOk());
    }

    @Test
    void shouldFilterByGenreAndAccentInsensitiveTitle_whenGuestSearches() throws Exception {
        String tur = "Tür-" + UUID.randomUUID();
        String ek = UUID.randomUUID().toString();
        long aranan = filmOlustur("Dövüş Kulübü " + ek, null, tur);
        filmOlustur("Başka Film " + ek, null, tur);

        mockMvc.perform(get(KATALOG).param("genre", tur).param("q", "dovus kulubu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(aranan))
                .andExpect(jsonPath("$.content[0].genres", hasItem(tur)));
        mockMvc.perform(get(KATALOG).param("genre", tur).param("size", "1"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void shouldReplaceFieldsAndRejectTmdbIdChange_whenMovieUpdated() throws Exception {
        long tmdbId = rastgeleTmdbId();
        long filmId = filmOlustur("Eski Başlık", tmdbId, "Dram");

        yonetim(put(YONETIM + "/" + filmId), filmGovdesi("Yeni Başlık", tmdbId, "Gerilim")
                        .replace("\"Brad Pitt\",", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Yeni Başlık"))
                .andExpect(jsonPath("$.genres", contains("Gerilim")))
                .andExpect(jsonPath("$.cast", contains("Edward Norton", "Helena Bonham Carter")));
        yonetim(put(YONETIM + "/" + filmId), filmGovdesi("Yeni Başlık", tmdbId + 1, "Dram"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("TMDB_ID_IMMUTABLE"));
    }

    @Test
    void shouldRejectInvalidInput_whenCertificationUnknownOrPageTooLarge() throws Exception {
        yonetim(post(YONETIM), filmGovdesi("Film", null, "Dram").replace("\"18+\"", "\"R\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("certification")));
        mockMvc.perform(get(KATALOG).param("size", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldForbidRegularUser_whenCreatingMovie() throws Exception {
        mockMvc.perform(post(YONETIM).header("Authorization", "Bearer " + TestTokenUretici.kullaniciTokeni(2, "USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(filmGovdesi("Film", null, "Dram")))
                .andExpect(status().isForbidden());
    }

    private long filmOlustur(String baslik, Long tmdbId, String tur) throws Exception {
        String govde = yonetim(post(YONETIM), filmGovdesi(baslik, tmdbId, tur))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(govde, "$.id")).longValue();
    }

    private static String filmGovdesi(String baslik, Long tmdbId, String tur) {
        return """
                {"tmdbId":%s,"title":"%s","overview":"Bir uykusuzluk hikayesi.",
                 "posterPath":"/yjMuqAyJUoQZGWsZ0vZuYj5inAR.jpg","runtimeMinutes":139,"releaseDate":"1999-10-15",
                 "certification":"18+","voteAverage":8.4,"director":"David Fincher","genres":["%s"],
                 "cast":["Brad Pitt","Edward Norton","Helena Bonham Carter"],
                 "contentDescriptors":["SIDDET_KORKU","CINSELLIK"]}"""
                .formatted(tmdbId, baslik, tur);
    }

    /** Testler aynı veritabanını paylaşır; TMDB id'leri çakışmasın. */
    private static long rastgeleTmdbId() {
        return ThreadLocalRandom.current().nextLong(1_000_000, 9_000_000_000L);
    }

    private ResultActions yonetim(MockHttpServletRequestBuilder istek, String govde) throws Exception {
        istek.header("Authorization", ADMIN_TOKENI);
        if (govde != null) {
            istek.contentType(MediaType.APPLICATION_JSON).content(govde);
        }
        return mockMvc.perform(istek);
    }
}
