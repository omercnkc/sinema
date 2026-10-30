package com.sinema.catalog.tmdb.istemci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sinema.catalog.config.TmdbAyarlari;
import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import java.time.Duration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Gerçek TMDB'ye gitmeden, HTTP katmanı sahte sunucuyla (MockRestServiceServer) sınanır. */
class TmdbIstemcisiTest {

    private static final String TABAN = "https://api.themoviedb.org/3";

    // Gerçek cevaptan kısaltılmış örnek (550, tr-TR); bilinmeyen alanlar (adult, budget...) yok sayılmalı
    private static final String FILM_JSON = """
            {"adult":false,"budget":63000000,"id":550,"title":"Dövüş Kulübü","overview":"Uykusuzluk...",
             "poster_path":"/yjMuqAyJUoQZGWsZ0vZuYj5inAR.jpg","runtime":139,"release_date":"1999-10-15",
             "vote_average":8.438,"genres":[{"id":18,"name":"Dram"}],
             "credits":{"cast":[{"name":"Brad Pitt","order":0}],"crew":[{"name":"David Fincher","job":"Director"}]},
             "release_dates":{"results":[{"iso_3166_1":"TR","release_dates":[
               {"certification":"18+","descriptors":["Violence / Horror"],"type":3,"note":""}]}]}}""";

    private final RestClient.Builder kurucu = RestClient.builder().baseUrl(TABAN);
    private final MockRestServiceServer sahteTmdb = MockRestServiceServer.bindTo(kurucu).build();

    @Test
    void shouldRequestDetailsWithAppendedResourcesAndParseResponse_whenMovieExists() {
        sahteTmdb.expect(requestTo(Matchers.startsWith(TABAN + "/movie/550")))
                .andExpect(queryParam("append_to_response", "release_dates,credits"))
                .andExpect(queryParam("language", "tr-TR"))
                .andRespond(withSuccess(FILM_JSON, MediaType.APPLICATION_JSON));

        TmdbFilmCevabi film = istemci("token").filmGetir(550);

        assertThat(film.title()).isEqualTo("Dövüş Kulübü");
        assertThat(film.releaseDates().results().getFirst().vizyonlar().getFirst().descriptors())
                .containsExactly("Violence / Horror");
        sahteTmdb.verify();
    }

    @Test
    void shouldSendQueryAndPage_whenSearching() {
        sahteTmdb.expect(requestTo(Matchers.startsWith(TABAN + "/search/movie")))
                .andExpect(queryParam("query", "D%C3%B6v%C3%BC%C5%9F"))
                .andExpect(queryParam("page", "2"))
                .andRespond(withSuccess("""
                        {"page":2,"total_pages":3,"total_results":41,
                         "results":[{"id":550,"title":"Dövüş Kulübü","original_title":"Fight Club",
                                     "release_date":"1999-10-15","poster_path":"/a.jpg"}]}""",
                        MediaType.APPLICATION_JSON));

        TmdbAramaCevabi cevap = istemci("token").ara("Dövüş", 2);

        assertThat(cevap.totalResults()).isEqualTo(41);
        assertThat(cevap.results().getFirst().originalTitle()).isEqualTo("Fight Club");
    }

    @Test
    void shouldThrowNotFound_whenTmdbReturns404() {
        sahteTmdb.expect(requestTo(Matchers.startsWith(TABAN + "/movie/1"))).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> istemci("token").filmGetir(1))
                .isInstanceOf(KaynakBulunamadiIstisnasi.class)
                .hasFieldOrPropertyWithValue("kod", "TMDB_MOVIE_NOT_FOUND");
    }

    @Test
    void shouldThrowUnavailable_whenTmdbFails() {
        sahteTmdb.expect(requestTo(Matchers.startsWith(TABAN + "/movie/1"))).andRespond(withServerError());

        assertThatThrownBy(() -> istemci("token").filmGetir(1))
                .isInstanceOf(BagimlilikKullanilamazIstisnasi.class)
                .hasFieldOrPropertyWithValue("kod", "TMDB_UNAVAILABLE");
    }

    @Test
    void shouldNotCallTmdb_whenTokenNotConfigured() {
        assertThatThrownBy(() -> istemci("").filmGetir(550)).isInstanceOf(BagimlilikKullanilamazIstisnasi.class);
        sahteTmdb.verify(); // hiç istek beklenmiyordu
    }

    private TmdbIstemcisi istemci(String token) {
        TmdbAyarlari ayarlar = new TmdbAyarlari(TABAN, token, "tr-TR", "TR", Duration.ofSeconds(1),
                Duration.ofSeconds(1), 20, Duration.ofDays(90), "-", 10);
        return new TmdbIstemcisi(kurucu.build(), ayarlar);
    }
}
