package com.sinema.catalog.seans.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import com.sinema.testdestegi.TestTokenUretici;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Her test kendi salonunu ve filmini kurar: testler aynı DB'yi paylaşsa da seansları çakışmaz. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MysqlTestYapilandirmasi.class)
class SeansKontrolcusuEntegrasyonTest {

    private static final String ADMIN_TOKENI = "Bearer " + TestTokenUretici.kullaniciTokeni(1, "ADMIN");
    private static final String SEANSLAR = "/api/v1/admin/catalog/showtimes";
    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private static final int FILM_SURESI = 100;
    private static final int SEANS_ARASI_PAYI = 15;

    private static final String DUZEN = """
            {"gridRows":2,"gridCols":2,"cells":[
              {"row":0,"col":0,"type":"SCREEN"},
              {"row":1,"col":0,"type":"SEAT","seatRowLabel":"A","seatNumber":1,"seatType":"STANDARD"}]}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long subeId;
    private long salonId;
    private long duzenId;
    private long filmId;
    private Instant baslangic;

    @BeforeEach
    void hazirla() throws Exception {
        subeId = idOku(yonetim(post("/api/v1/admin/catalog/cinemas"),
                "{\"name\":\"Şube %s\",\"city\":\"İstanbul\",\"address\":\"Adres\"}".formatted(UUID.randomUUID())));
        salonId = idOku(yonetim(post("/api/v1/admin/catalog/halls"),
                "{\"cinemaId\":%d,\"name\":\"Salon 1\",\"defaultTicketPrice\":\"120\"}".formatted(subeId)));
        duzenId = duzenYayimla(salonId);
        filmId = filmOlustur();
        baslangic = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
    }

    @Test
    void shouldCopyHallPriceAndSealActiveLayout_whenShowtimeCreated() throws Exception {
        seansOlustur(baslangic, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endsAt").value(baslangic.plus(FILM_SURESI + SEANS_ARASI_PAYI,
                        ChronoUnit.MINUTES).toString()))
                .andExpect(jsonPath("$.ticketPrice").value("120.00"))
                .andExpect(jsonPath("$.layoutId").value(duzenId))
                .andExpect(jsonPath("$.salesCloseOffsetMinutes").value(10))
                .andExpect(jsonPath("$.salesCloseAt").value(baslangic.plus(10, ChronoUnit.MINUTES).toString()))
                .andExpect(jsonPath("$.salesOpen").value(true))
                .andExpect(jsonPath("$.serverTime").isNotEmpty());
    }

    @Test
    void shouldRejectOverlapButAllowBackToBack_whenHallAlreadyHasShowtime() throws Exception {
        long ilkSeans = idOku(seansOlustur(baslangic, null));
        Instant ilkBitis = baslangic.plus(FILM_SURESI + SEANS_ARASI_PAYI, ChronoUnit.MINUTES);

        seansOlustur(baslangic.plus(60, ChronoUnit.MINUTES), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHOWTIME_OVERLAP"))
                .andExpect(jsonPath("$.conflictingShowtimeId").value(ilkSeans));
        seansOlustur(ilkBitis.minus(1, ChronoUnit.MINUTES), null).andExpect(status().isConflict());
        // Aralıklar yarı açık: önceki seansın (ara payı dahil) bittiği an başlayabilir
        seansOlustur(ilkBitis, null).andExpect(status().isCreated());
    }

    @Test
    void shouldRejectShowtime_whenInPastOrMovieHiddenOrHallHasNoLayout() throws Exception {
        seansOlustur(Instant.now().minus(1, ChronoUnit.MINUTES), null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SHOWTIME_IN_PAST"));

        long duzensizSalon = idOku(yonetim(post("/api/v1/admin/catalog/halls"),
                "{\"cinemaId\":%d,\"name\":\"Salon 2\",\"defaultTicketPrice\":\"100\"}".formatted(subeId)));
        yonetim(post(SEANSLAR), seansGovdesi(filmId, duzensizSalon, baslangic, null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("HALL_HAS_NO_LAYOUT"));

        yonetim(patch("/api/v1/admin/catalog/movies/" + filmId + "/visibility"), "{\"visible\":false}");
        seansOlustur(baslangic, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("MOVIE_HIDDEN"));
    }

    @Test
    void shouldKeepSealedLayoutForExistingShowtime_whenNewLayoutPublished() throws Exception {
        long eskiSeans = idOku(seansOlustur(baslangic, null));
        long yeniDuzen = duzenYayimla(salonId);

        mockMvc.perform(get("/api/v1/catalog/showtimes/" + eskiSeans + "/layout"))
                .andExpect(jsonPath("$.layoutId").value(duzenId))
                .andExpect(jsonPath("$.version").value(1));
        seansOlustur(baslangic.plus(1, ChronoUnit.DAYS), null)
                .andExpect(jsonPath("$.layoutId").value(yeniDuzen));
    }

    @Test
    void shouldGroupByIstanbulDate_whenShowtimeStartsAfterMidnightLocalTime() throws Exception {
        LocalDate gun = LocalDate.now(ISTANBUL).plusDays(20);
        // İstanbul'da gece 00:30 = UTC'de önceki gün 21:30
        Instant geceYarisindanSonra = gun.atTime(LocalTime.of(0, 30)).atZone(ISTANBUL).toInstant();
        long seansId = idOku(seansOlustur(geceYarisindanSonra, null));

        mockMvc.perform(get("/api/v1/catalog/movies/" + filmId + "/showtimes").param("date", gun.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(seansId))
                .andExpect(jsonPath("$[0].cinemaId").value(subeId))
                .andExpect(jsonPath("$[0].hallName").value("Salon 1"));
        mockMvc.perform(get("/api/v1/catalog/movies/" + filmId + "/showtimes")
                        .param("date", gun.minusDays(1).toString()))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/catalog/movies/" + filmId + "/showtimes")
                        .param("date", gun.toString()).param("cinemaId", "999999"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturnNotModified_whenLayoutRequestedWithSameEtag() throws Exception {
        long seansId = idOku(seansOlustur(baslangic, null));

        String etiket = mockMvc.perform(get("/api/v1/catalog/showtimes/" + seansId + "/layout"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=300, public"))
                .andExpect(jsonPath("$.cells", hasSize(2)))
                .andReturn().getResponse().getHeader("ETag");
        mockMvc.perform(get("/api/v1/catalog/showtimes/" + seansId + "/layout").header("If-None-Match", etiket))
                .andExpect(status().isNotModified());
    }

    @Test
    void shouldRefusePublishing_whenShowtimeRunningInHall() throws Exception {
        // API geçmişe seans eklemez; süren seans doğrudan DB'ye yazılır
        Instant simdi = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO showtimes (movie_id, hall_id, layout_id, starts_at, ends_at, ticket_price,
                    sales_close_offset_minutes, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 100, 10, 'SCHEDULED', NOW(3), NOW(3))""",
                filmId, salonId, duzenId, Timestamp.from(simdi.minus(30, ChronoUnit.MINUTES)),
                Timestamp.from(simdi.plus(60, ChronoUnit.MINUTES)));
        long taslakId = idOku(yonetim(post("/api/v1/admin/catalog/halls/" + salonId + "/layouts"), DUZEN));

        yonetim(post("/api/v1/admin/catalog/halls/" + salonId + "/layouts/" + taslakId + "/publish"), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HALL_SHOWTIME_IN_PROGRESS"));
    }

    @Test
    void shouldUpdatePriceAndOffset_andKeepDetailVisible_whenMovieLaterHidden() throws Exception {
        long seansId = idOku(seansOlustur(baslangic, "\"95.50\""));

        yonetim(put(SEANSLAR + "/" + seansId), "{\"ticketPrice\":\"110\",\"salesCloseOffsetMinutes\":-15}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketPrice").value("110.00"))
                .andExpect(jsonPath("$.salesCloseAt").value(baslangic.minus(15, ChronoUnit.MINUTES).toString()));
        yonetim(patch("/api/v1/admin/catalog/movies/" + filmId + "/visibility"), "{\"visible\":false}");

        mockMvc.perform(get("/api/v1/catalog/showtimes/" + seansId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketPrice").value("110.00"));
    }

    private ResultActions seansOlustur(Instant an, String fiyatJson) throws Exception {
        return yonetim(post(SEANSLAR), seansGovdesi(filmId, salonId, an, fiyatJson));
    }

    private static String seansGovdesi(long film, long salon, Instant an, String fiyatJson) {
        return "{\"movieId\":%d,\"hallId\":%d,\"startsAt\":\"%s\",\"ticketPrice\":%s}"
                .formatted(film, salon, an, fiyatJson);
    }

    private long duzenYayimla(long salon) throws Exception {
        String adres = "/api/v1/admin/catalog/halls/" + salon + "/layouts";
        long taslak = idOku(yonetim(post(adres), DUZEN));
        yonetim(post(adres + "/" + taslak + "/publish"), null).andExpect(status().isOk());
        return taslak;
    }

    private long filmOlustur() throws Exception {
        return idOku(yonetim(post("/api/v1/admin/catalog/movies"),
                "{\"title\":\"Film %s\",\"runtimeMinutes\":%d}".formatted(UUID.randomUUID(), FILM_SURESI)));
    }

    private static long idOku(ResultActions sonuc) throws Exception {
        return ((Number) JsonPath.read(sonuc.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private ResultActions yonetim(MockHttpServletRequestBuilder istek, String govde) throws Exception {
        istek.header("Authorization", ADMIN_TOKENI);
        if (govde != null) {
            istek.contentType(MediaType.APPLICATION_JSON).content(govde);
        }
        return mockMvc.perform(istek);
    }
}
