package com.sinema.catalog.seans.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.catalog.KatalogTestVerisi;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import com.sinema.testdestegi.TestTokenUretici;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MysqlTestYapilandirmasi.class)
class SeansIcKontrolcusuEntegrasyonTest {

    private static final String BOOKING_TOKENI =
            "Bearer " + TestTokenUretici.servisTokeni("booking-service", "catalog-service");

    @Autowired
    private MockMvc mockMvc;

    private KatalogTestVerisi veri;
    private long salonId;
    private long seansId;
    private Instant baslangic;

    @BeforeEach
    void hazirla() throws Exception {
        veri = new KatalogTestVerisi(mockMvc);
        salonId = veri.salonOlustur(veri.subeOlustur(), "150");
        veri.duzenYayimla(salonId, KatalogTestVerisi.KUCUK_DUZEN);
        baslangic = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        seansId = veri.seansOlustur(veri.filmOlustur(90), salonId, baslangic);
    }

    @Test
    void shouldReturnSnapshotFieldsAndSalesClose_whenBookingServiceAsks() throws Exception {
        mockMvc.perform(get(icAdres()).header("Authorization", BOOKING_TOKENI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showtimeId").value(seansId))
                .andExpect(jsonPath("$.hallId").value(salonId))
                .andExpect(jsonPath("$.movieTitle").isNotEmpty())
                .andExpect(jsonPath("$.cinemaName").isNotEmpty())
                .andExpect(jsonPath("$.ticketPrice").value("150.00"))
                .andExpect(jsonPath("$.salesCloseAt").value(baslangic.plus(10, ChronoUnit.MINUTES).toString()))
                .andExpect(jsonPath("$.serverTime").isNotEmpty());
    }

    @Test
    void shouldSplitSeatsAndInvalidCells_whenResolvingCellIds() throws Exception {
        List<Integer> koltuklar = hucreIdleri("SEAT");
        int perde = hucreIdleri("SCREEN").getFirst();
        long baskaDuzeninHucresi = Long.MAX_VALUE;

        koltuklariCoz(List.of(koltuklar.get(1), perde, koltuklar.get(0), baskaDuzeninHucresi), BOOKING_TOKENI)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats[*].label", contains("A-2", "A-1")))
                .andExpect(jsonPath("$.seats[0].seatType").value("ACCESSIBLE"))
                .andExpect(jsonPath("$.invalidCellIds[0]").value(perde))
                .andExpect(jsonPath("$.invalidCellIds[1]").value(baskaDuzeninHucresi));
    }

    @Test
    void shouldRejectCellsOfNewLayout_whenShowtimeSealedToOldOne() throws Exception {
        veri.duzenYayimla(salonId, KatalogTestVerisi.KUCUK_DUZEN);
        String yeniDuzen = mockMvc.perform(get("/api/v1/catalog/showtimes/" + veri.seansOlustur(
                        veri.filmOlustur(90), salonId, baslangic.plus(1, ChronoUnit.DAYS)) + "/layout"))
                .andReturn().getResponse().getContentAsString();
        List<Integer> yeniKoltuklar = JsonPath.read(yeniDuzen, "$.cells[?(@.type == 'SEAT')].cellId");

        koltuklariCoz(List.of(yeniKoltuklar.getFirst()), BOOKING_TOKENI)
                .andExpect(jsonPath("$.seats").isEmpty())
                .andExpect(jsonPath("$.invalidCellIds[0]").value(yeniKoltuklar.getFirst()));
    }

    @Test
    void shouldAllowOnlyServiceTokenForCatalog_onInternalEndpoints() throws Exception {
        mockMvc.perform(get(icAdres())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(icAdres()).header("Authorization", KatalogTestVerisi.ADMIN_TOKENI))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(icAdres()).header("Authorization",
                        "Bearer " + TestTokenUretici.servisTokeni("payment-service", "booking-service")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundOrBadRequest_whenShowtimeMissingOrBodyEmpty() throws Exception {
        mockMvc.perform(get("/internal/v1/catalog/showtimes/999999").header("Authorization", BOOKING_TOKENI))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHOWTIME_NOT_FOUND"));
        koltuklariCoz(List.of(), BOOKING_TOKENI).andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions koltuklariCoz(List<? extends Number> hucreler,
            String token) throws Exception {
        return mockMvc.perform(post(icAdres() + "/seats/resolve").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cellIds\":" + hucreler + "}"));
    }

    private List<Integer> hucreIdleri(String tip) throws Exception {
        String duzen = mockMvc.perform(get("/api/v1/catalog/showtimes/" + seansId + "/layout"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(duzen, "$.cells[?(@.type == '%s')].cellId".formatted(tip));
    }

    private String icAdres() {
        return "/internal/v1/catalog/showtimes/" + seansId;
    }
}
