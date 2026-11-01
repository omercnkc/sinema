package com.sinema.catalog.duzen.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sinema.catalog.KatalogTestVerisi;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import com.sinema.testdestegi.TestTokenUretici;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(MysqlTestYapilandirmasi.class)
class DuzenIcKontrolcusuEntegrasyonTest {

    private static final String BOOKING_TOKENI =
            "Bearer " + TestTokenUretici.servisTokeni("booking-service", "catalog-service");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private KatalogTestVerisi veri;
    private long salonId;
    private long eskiDuzen;
    private long filmId;
    private long yarinkiSeans;
    private long sonrakiSeans;
    private long gecmisSeans;
    private long taslak;

    @BeforeEach
    void hazirla() throws Exception {
        veri = new KatalogTestVerisi(mockMvc);
        salonId = veri.salonOlustur(veri.subeOlustur(), "150");
        eskiDuzen = veri.duzenYayimla(salonId, KatalogTestVerisi.KUCUK_DUZEN);
        filmId = veri.filmOlustur(90);
        Instant yarin = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        yarinkiSeans = veri.seansOlustur(filmId, salonId, yarin);
        sonrakiSeans = veri.seansOlustur(filmId, salonId, yarin.plus(1, ChronoUnit.DAYS));
        gecmisSeans = gecmisSeansEkle();
        taslak = KatalogTestVerisi.idOku(veri.yonetim(post(duzenAdresi()), KatalogTestVerisi.KUCUK_DUZEN));
    }

    @Test
    void shouldReturnTargetDraftUnstartedShowtimesAndTheirLayouts_whenBookingAsksMigrationSource() throws Exception {
        mockMvc.perform(get("/internal/v1/catalog/halls/" + salonId + "/migration-source")
                        .param("toLayoutId", String.valueOf(taslak)).header("Authorization", BOOKING_TOKENI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hallId").value(salonId))
                .andExpect(jsonPath("$.targetLayout.layoutId").value(taslak))
                .andExpect(jsonPath("$.targetLayout.cells", hasSize(5)))
                .andExpect(jsonPath("$.showtimes[*].id", containsInAnyOrder((int) yarinkiSeans, (int) sonrakiSeans)))
                .andExpect(jsonPath("$.showtimes[0].layoutId").value(eskiDuzen))
                .andExpect(jsonPath("$.sourceLayouts", hasSize(1)))
                .andExpect(jsonPath("$.sourceLayouts[0].layoutId").value(eskiDuzen))
                .andExpect(jsonPath("$.sourceLayouts[0].cells[2].seatRowLabel").value("A"));
    }

    @Test
    void shouldRequireMigration_whenAdminPublishesHallWithUnstartedShowtimes() throws Exception {
        veri.yonetim(post(duzenAdresi() + "/" + taslak + "/publish"), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAYOUT_MIGRATION_REQUIRED"));
    }

    @Test
    void shouldPublishAndMoveUnstartedShowtimesOnly_whenActivated_andRepeatSafely() throws Exception {
        etkinlestir(taslak, "[" + yarinkiSeans + "]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layoutId").value(taslak))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.migratedShowtimeCount").value(2));

        seansDuzeni(yarinkiSeans).andExpect(jsonPath("$.layoutId").value(taslak));
        seansDuzeni(sonrakiSeans).andExpect(jsonPath("$.layoutId").value(taslak));
        seansDuzeni(gecmisSeans).andExpect(jsonPath("$.layoutId").value(eskiDuzen));
        veri.yonetim(get(duzenAdresi()), null).andExpect(jsonPath("$[0].active").value(true));

        etkinlestir(taslak, "[" + yarinkiSeans + "]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.migratedShowtimeCount").value(0));
    }

    @Test
    void shouldRefuseActivation_whenPlannedShowtimeAlreadyStarted() throws Exception {
        etkinlestir(taslak, "[" + yarinkiSeans + "," + gecmisSeans + "]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MIGRATION_SHOWTIMES_CHANGED"))
                .andExpect(jsonPath("$.showtimeIds[0]").value(gecmisSeans));

        seansDuzeni(yarinkiSeans).andExpect(jsonPath("$.layoutId").value(eskiDuzen));
        veri.yonetim(get(duzenAdresi()), null).andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void shouldRejectMigrationSource_whenTargetAlreadyPublishedOrCallerNotService() throws Exception {
        mockMvc.perform(get("/internal/v1/catalog/halls/" + salonId + "/migration-source")
                        .param("toLayoutId", String.valueOf(eskiDuzen)).header("Authorization", BOOKING_TOKENI))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAYOUT_ALREADY_PUBLISHED"));
        mockMvc.perform(get("/internal/v1/catalog/halls/" + salonId + "/migration-source")
                        .param("toLayoutId", String.valueOf(taslak)).header("Authorization", KatalogTestVerisi.ADMIN_TOKENI))
                .andExpect(status().isForbidden());
    }

    private ResultActions etkinlestir(long duzenId, String seansIdleri) throws Exception {
        return mockMvc.perform(post("/internal/v1/catalog/halls/" + salonId + "/layouts/" + duzenId + "/activate")
                .header("Authorization", BOOKING_TOKENI)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"showtimeIds\":" + seansIdleri + "}"));
    }

    private ResultActions seansDuzeni(long seansId) throws Exception {
        return mockMvc.perform(get("/api/v1/catalog/showtimes/" + seansId + "/layout"));
    }

    private String duzenAdresi() {
        return "/api/v1/admin/catalog/halls/" + salonId + "/layouts";
    }

    /** API geçmişe seans eklemez; bitmiş seans doğrudan DB'ye yazılır (K13'ü tetiklemez, göçte eski sürümde kalır). */
    private long gecmisSeansEkle() {
        Instant simdi = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO showtimes (movie_id, hall_id, layout_id, starts_at, ends_at, ticket_price,
                    sales_close_offset_minutes, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 100, 10, 'SCHEDULED', NOW(3), NOW(3))""",
                filmId, salonId, eskiDuzen, Timestamp.from(simdi.minus(5, ChronoUnit.HOURS)),
                Timestamp.from(simdi.minus(3, ChronoUnit.HOURS)));
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM showtimes WHERE hall_id = ?", Long.class, salonId);
    }
}
