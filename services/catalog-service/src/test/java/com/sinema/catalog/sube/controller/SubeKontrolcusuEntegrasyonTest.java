package com.sinema.catalog.sube.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import com.sinema.testdestegi.TestTokenUretici;
import java.util.UUID;
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
class SubeKontrolcusuEntegrasyonTest {

    private static final String ADMIN_TOKENI = "Bearer " + TestTokenUretici.kullaniciTokeni(1, "ADMIN");
    private static final String KULLANICI_TOKENI = "Bearer " + TestTokenUretici.kullaniciTokeni(2, "USER");
    private static final String YONETIM = "/api/v1/admin/catalog";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldListCinemaWithHallsWithoutPrice_whenGuestCallsPublicEndpoint() throws Exception {
        long subeId = subeOlustur("Kadıköy " + UUID.randomUUID());
        salonOlustur(subeId, "Salon 1", "\"120\"");

        mockMvc.perform(get("/api/v1/catalog/cinemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].halls[0].name".formatted(subeId)).value(hasItem("Salon 1")))
                .andExpect(jsonPath("$[?(@.id == %d)].halls[0].defaultTicketPrice".formatted(subeId)).isEmpty());
    }

    @Test
    void shouldReturnPriceAsTwoDecimalString_whenAdminCreatesHall() throws Exception {
        long subeId = subeOlustur("Beşiktaş " + UUID.randomUUID());

        yonetim(post(YONETIM + "/halls"), salonGovdesi(subeId, "Salon 1", "120"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cinemaId").value(subeId))
                .andExpect(jsonPath("$.defaultTicketPrice").value("120.00"))
                .andExpect(jsonPath("$.activeLayoutId").isEmpty());
    }

    @Test
    void shouldUpdateCinemaAndHall_whenAdminSendsValidChanges() throws Exception {
        long subeId = subeOlustur("Şişli " + UUID.randomUUID());
        long salonId = salonOlustur(subeId, "Salon 1", "\"100.00\"");

        yonetim(put(YONETIM + "/cinemas/" + subeId), "{\"name\":\"Yeni Ad\",\"city\":\"İstanbul\",\"address\":\"Yeni adres\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yeni Ad"))
                .andExpect(jsonPath("$.halls[0].id").value(salonId));
        yonetim(put(YONETIM + "/halls/" + salonId), "{\"name\":\"Salon A\",\"defaultTicketPrice\":\"135.50\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Salon A"))
                .andExpect(jsonPath("$.defaultTicketPrice").value("135.50"));
    }

    @Test
    void shouldReturnConflict_whenHallNameAlreadyUsedInSameCinema() throws Exception {
        long subeId = subeOlustur("Bakırköy " + UUID.randomUUID());
        salonOlustur(subeId, "Salon 1", "\"100\"");
        long ikinciSalonId = salonOlustur(subeId, "Salon 2", "\"100\"");

        yonetim(post(YONETIM + "/halls"), salonGovdesi(subeId, " Salon 1 ", "100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HALL_NAME_TAKEN"));
        yonetim(put(YONETIM + "/halls/" + ikinciSalonId), "{\"name\":\"Salon 1\",\"defaultTicketPrice\":\"100\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void shouldAllowSameHallName_whenCinemasDiffer() throws Exception {
        salonOlustur(subeOlustur("A " + UUID.randomUUID()), "Salon 1", "\"100\"");
        salonOlustur(subeOlustur("B " + UUID.randomUUID()), "Salon 1", "\"100\"");
    }

    @Test
    void shouldRejectPrice_whenNegativeOrHasMoreThanTwoDecimals() throws Exception {
        long subeId = subeOlustur("Üsküdar " + UUID.randomUUID());

        yonetim(post(YONETIM + "/halls"), salonGovdesi(subeId, "Salon 1", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("defaultTicketPrice")));
        yonetim(post(YONETIM + "/halls"), salonGovdesi(subeId, "Salon 1", "10.555"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFound_whenCinemaOrHallMissing() throws Exception {
        yonetim(post(YONETIM + "/halls"), salonGovdesi(999_999, "Salon 1", "100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CINEMA_NOT_FOUND"));
        yonetim(put(YONETIM + "/halls/999999"), "{\"name\":\"X\",\"defaultTicketPrice\":\"1\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HALL_NOT_FOUND"));
    }

    @Test
    void shouldForbidRegularUserAndRejectGuest_onAdminEndpoints() throws Exception {
        String govde = "{\"name\":\"X\",\"city\":\"İstanbul\",\"address\":\"Y\"}";

        mockMvc.perform(post(YONETIM + "/cinemas").header("Authorization", KULLANICI_TOKENI)
                        .contentType(MediaType.APPLICATION_JSON).content(govde))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(YONETIM + "/cinemas").contentType(MediaType.APPLICATION_JSON).content(govde))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListHallsWithPrice_whenAdminListsCinemas() throws Exception {
        long subeId = subeOlustur("Ataşehir " + UUID.randomUUID());
        salonOlustur(subeId, "Salon 1", "\"99.90\"");

        yonetim(get(YONETIM + "/cinemas"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].halls[0].defaultTicketPrice".formatted(subeId)).value(hasItem("99.90")));
    }

    private long subeOlustur(String ad) throws Exception {
        String govde = yonetim(post(YONETIM + "/cinemas"),
                        "{\"name\":\"%s\",\"city\":\"İstanbul\",\"address\":\"Örnek cad. 1\"}".formatted(ad))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(govde, "$.id")).longValue();
    }

    private long salonOlustur(long subeId, String ad, String fiyatJson) throws Exception {
        String govde = yonetim(post(YONETIM + "/halls"),
                        "{\"cinemaId\":%d,\"name\":\"%s\",\"defaultTicketPrice\":%s}".formatted(subeId, ad, fiyatJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(govde, "$.id")).longValue();
    }

    private static String salonGovdesi(long subeId, String ad, String fiyat) {
        return "{\"cinemaId\":%d,\"name\":\"%s\",\"defaultTicketPrice\":\"%s\"}".formatted(subeId, ad, fiyat);
    }

    private ResultActions yonetim(MockHttpServletRequestBuilder istek, String govde)
            throws Exception {
        istek.header("Authorization", ADMIN_TOKENI);
        if (govde != null) {
            istek.contentType(MediaType.APPLICATION_JSON).content(govde);
        }
        return mockMvc.perform(istek);
    }
}
