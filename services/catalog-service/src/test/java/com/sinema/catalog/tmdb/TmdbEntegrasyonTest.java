package com.sinema.catalog.tmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.catalog.film.entity.AkilliIsaret;
import com.sinema.catalog.film.entity.Film;
import com.sinema.catalog.film.repository.FilmRepository;
import com.sinema.catalog.tmdb.istemci.TmdbAramaCevabi;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi;
import com.sinema.catalog.tmdb.istemci.TmdbIstemcisi;
import com.sinema.catalog.tmdb.service.TmdbYenilemeServisi;
import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import com.sinema.testdestegi.TestTokenUretici;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

// Token dolu olmalı: yoksa yenileme job'ı hiç çalışmaz (TMDB istemcisi zaten sahte)
@SpringBootTest(properties = "sinema.tmdb.token=test-token")
@AutoConfigureMockMvc
@Import(MysqlTestYapilandirmasi.class)
class TmdbEntegrasyonTest {

    private static final String ADMIN_TOKENI = "Bearer " + TestTokenUretici.kullaniciTokeni(1, "ADMIN");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private TmdbYenilemeServisi yenilemeServisi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private TmdbIstemcisi tmdbIstemcisi;

    @Test
    void shouldMarkAlreadyAddedMovies_whenAdminSearchesTmdb() throws Exception {
        long ekliTmdbId = rastgeleTmdbId();
        long filmId = tmdbFilmiEkle(ekliTmdbId);
        long yeniTmdbId = rastgeleTmdbId();
        when(tmdbIstemcisi.ara("dövüş", 1)).thenReturn(new TmdbAramaCevabi(1, 1, 2, List.of(
                new TmdbAramaCevabi.Sonuc(ekliTmdbId, "Dövüş Kulübü", "Fight Club", "1999-10-15", "/a.jpg"),
                new TmdbAramaCevabi.Sonuc(yeniTmdbId, "Başka", "Other", "", null))));

        admin("/api/v1/admin/catalog/tmdb/search?query= dövüş ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].movieId").value(filmId))
                .andExpect(jsonPath("$.results[0].releaseDate").value("1999-10-15"))
                .andExpect(jsonPath("$.results[1].movieId").isEmpty())
                .andExpect(jsonPath("$.results[1].releaseDate").isEmpty());
    }

    @Test
    void shouldReturnPrefilledFormThatCanBeSavedAsIs_whenAdminPicksTmdbMovie() throws Exception {
        long tmdbId = rastgeleTmdbId();
        when(tmdbIstemcisi.filmGetir(tmdbId)).thenReturn(tmdbFilmi(tmdbId, "Dövüş Kulübü", "Özet", "18+"));

        String form = admin("/api/v1/admin/catalog/tmdb/movies/" + tmdbId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certification").value("18+"))
                .andExpect(jsonPath("$.contentDescriptors[0]").value("SIDDET_KORKU"))
                .andExpect(jsonPath("$.movieId").isEmpty())
                .andReturn().getResponse().getContentAsString();

        // Önizleme FilmIstegi ile aynı biçimde: düzenlemeden kaydedilebilmeli
        mockMvc.perform(post("/api/v1/admin/catalog/movies").header("Authorization", ADMIN_TOKENI)
                        .contentType(MediaType.APPLICATION_JSON).content(form))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tmdbId").value(tmdbId));
    }

    @Test
    void shouldReturnServiceUnavailable_whenTmdbDown() throws Exception {
        when(tmdbIstemcisi.ara(anyString(), anyInt()))
                .thenThrow(new BagimlilikKullanilamazIstisnasi("TMDB_UNAVAILABLE", "TMDB'ye şu an ulaşılamıyor."));

        admin("/api/v1/admin/catalog/tmdb/search?query=x")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("TMDB_UNAVAILABLE"));
    }

    @Test
    void shouldRefreshStaleTmdbMovieKeepingAdminValuesForEmptyFields_whenRefreshRuns() {
        long tmdbId = rastgeleTmdbId();
        long filmId = tmdbFilmiEkle(tmdbId);
        senkronZamaniniGeriAl(filmId, 120);
        // TMDB'de özet ve TR yaş sınırı boş: admin'in girdiği değerler korunmalı
        when(tmdbIstemcisi.filmGetir(anyLong())).thenReturn(tmdbFilmi(tmdbId, "Yeni Başlık", "", null));

        yenilemeServisi.suresiGecenleriYenile();

        Film film = filmiYukle(filmId);
        assertThat(film.getBaslik()).isEqualTo("Yeni Başlık");
        assertThat(film.getOzet()).isEqualTo("Admin özeti");
        assertThat(film.getYasSiniri()).isEqualTo("16+");
        assertThat(film.getAkilliIsaretler()).containsExactly(AkilliIsaret.SIDDET_KORKU);
        assertThat(film.getTmdbSenkronZamani()).isAfter(Instant.now().minus(1, ChronoUnit.MINUTES));
    }

    @Test
    void shouldLeaveFreshMovieAndStopQuietly_whenTmdbUnavailable() {
        long tazeFilm = tmdbFilmiEkle(rastgeleTmdbId());
        long eskiFilm = tmdbFilmiEkle(rastgeleTmdbId());
        senkronZamaniniGeriAl(eskiFilm, 120);
        when(tmdbIstemcisi.filmGetir(anyLong()))
                .thenThrow(new BagimlilikKullanilamazIstisnasi("TMDB_UNAVAILABLE", "x"));

        yenilemeServisi.suresiGecenleriYenile();

        assertThat(filmiYukle(eskiFilm).getBaslik()).as("hata olunca dokunulmaz").isEqualTo("Admin başlığı");
        assertThat(filmiYukle(tazeFilm).getTmdbSenkronZamani())
                .isAfter(Instant.now().minus(1, ChronoUnit.MINUTES));
    }

    private long tmdbFilmiEkle(long tmdbId) {
        String govde = """
                {"tmdbId":%d,"title":"Admin başlığı","overview":"Admin özeti","runtimeMinutes":120,
                 "certification":"16+","contentDescriptors":["SIDDET_KORKU"],"genres":["Dram"]}""".formatted(tmdbId);
        try {
            String cevap = mockMvc.perform(post("/api/v1/admin/catalog/movies").header("Authorization", ADMIN_TOKENI)
                            .contentType(MediaType.APPLICATION_JSON).content(govde))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            return ((Number) JsonPath.read(cevap, "$.id")).longValue();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void senkronZamaniniGeriAl(long filmId, int gun) {
        jdbcTemplate.update("UPDATE movies SET tmdb_synced_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(gun, ChronoUnit.DAYS)), filmId);
    }

    /** Lazy koleksiyonlar transaction içinde okunur. */
    private Film filmiYukle(long filmId) {
        return transactionTemplate.execute(durum -> {
            Film film = filmRepository.findById(filmId).orElseThrow();
            film.getAkilliIsaretler().size();
            return film;
        });
    }

    private static TmdbFilmCevabi tmdbFilmi(long tmdbId, String baslik, String ozet, String trSertifikasi) {
        List<TmdbFilmCevabi.Vizyon> vizyonlar = trSertifikasi == null ? List.of()
                : List.of(new TmdbFilmCevabi.Vizyon(trSertifikasi, List.of("Violence / Horror"), 3));
        return new TmdbFilmCevabi(tmdbId, baslik, ozet, "/a.jpg", 139, "1999-10-15", 8.4,
                List.of(new TmdbFilmCevabi.Ad("Dram")),
                new TmdbFilmCevabi.Kadro(List.of(new TmdbFilmCevabi.Oyuncu("Brad Pitt", 0)), List.of()),
                new TmdbFilmCevabi.VizyonKayitlari(List.of(new TmdbFilmCevabi.UlkeVizyonu("TR", vizyonlar))));
    }

    private org.springframework.test.web.servlet.ResultActions admin(String adres) throws Exception {
        return mockMvc.perform(get(adres).header("Authorization", ADMIN_TOKENI));
    }

    private static long rastgeleTmdbId() {
        return ThreadLocalRandom.current().nextLong(1_000_000, 9_000_000_000L);
    }
}
