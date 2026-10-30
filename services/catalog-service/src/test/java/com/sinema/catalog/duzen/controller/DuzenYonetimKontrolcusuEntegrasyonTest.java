package com.sinema.catalog.duzen.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import com.sinema.testdestegi.TestTokenUretici;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class DuzenYonetimKontrolcusuEntegrasyonTest {

    private static final String ADMIN_TOKENI = "Bearer " + TestTokenUretici.kullaniciTokeni(1, "ADMIN");

    // 3x4 ızgara: üstte perde, altında iki sıra koltuk (biri erişilebilir), sağ altta çıkış
    private static final String GECERLI_DUZEN = """
            {"gridRows":3,"gridCols":4,"cells":[
              {"row":0,"col":0,"type":"SCREEN"},{"row":0,"col":1,"type":"SCREEN"},
              {"row":1,"col":0,"type":"SEAT","seatRowLabel":"A","seatNumber":1,"seatType":"STANDARD"},
              {"row":1,"col":1,"type":"SEAT","seatRowLabel":"A","seatNumber":2,"seatType":"ACCESSIBLE"},
              {"row":2,"col":0,"type":"SEAT","seatRowLabel":"B","seatNumber":1,"seatType":"STANDARD"},
              {"row":2,"col":3,"type":"EXIT"}]}""";

    private static final String KUCUK_DUZEN = """
            {"gridRows":2,"gridCols":2,"cells":[
              {"row":0,"col":0,"type":"SCREEN"},
              {"row":1,"col":1,"type":"SEAT","seatRowLabel":"A","seatNumber":1,"seatType":"STANDARD"}]}""";

    @Autowired
    private MockMvc mockMvc;

    private String duzenAdresi;

    @BeforeEach
    void salonHazirla() throws Exception {
        duzenAdresi = "/api/v1/admin/catalog/halls/" + salonOlustur() + "/layouts";
    }

    @Test
    void shouldCreateDraftWithCellsInPositionOrder_whenLayoutValid() throws Exception {
        yonetim(post(duzenAdresi), GECERLI_DUZEN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").isEmpty())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.cells", hasSize(6)))
                .andExpect(jsonPath("$.cells[2].seatRowLabel").value("A"))
                .andExpect(jsonPath("$.cells[3].seatType").value("ACCESSIBLE"))
                .andExpect(jsonPath("$.cells[5].type").value("EXIT"));
    }

    @Test
    void shouldReturnConflictWithExistingDraftId_whenHallAlreadyHasDraft() throws Exception {
        long taslakId = taslakOlustur(GECERLI_DUZEN);

        yonetim(post(duzenAdresi), KUCUK_DUZEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DRAFT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.layoutId").value(taslakId));
    }

    @Test
    void shouldReplaceAllCells_whenDraftUpdated() throws Exception {
        long taslakId = taslakOlustur(GECERLI_DUZEN);

        yonetim(put(duzenAdresi + "/" + taslakId), KUCUK_DUZEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gridRows").value(2))
                .andExpect(jsonPath("$.cells", hasSize(2)));
        yonetim(get(duzenAdresi + "/" + taslakId), null)
                .andExpect(jsonPath("$.gridCols").value(2))
                .andExpect(jsonPath("$.cells", hasSize(2)));
    }

    @Test
    void shouldReturnViolations_whenLayoutBreaksRules() throws Exception {
        String perdesizCakisan = """
                {"gridRows":2,"gridCols":2,"cells":[
                  {"row":0,"col":0,"type":"SEAT","seatRowLabel":"A","seatNumber":1,"seatType":"STANDARD"},
                  {"row":0,"col":0,"type":"WALL"}]}""";

        yonetim(post(duzenAdresi), perdesizCakisan)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("LAYOUT_INVALID"))
                .andExpect(jsonPath("$.violations", hasItem("(0,0): aynı konumda birden fazla hücre var")))
                .andExpect(jsonPath("$.violations", hasItem("Düzende perde (SCREEN) olmalı")));
    }

    @Test
    void shouldReturnBadRequest_whenGridTooLargeOrLabelMalformed() throws Exception {
        yonetim(post(duzenAdresi), "{\"gridRows\":51,\"gridCols\":2,\"cells\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("gridRows")));
        yonetim(post(duzenAdresi), KUCUK_DUZEN.replace("\"A\"", "\"a1\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListDraftWithSeatCount_whenHallHasDraft() throws Exception {
        long taslakId = taslakOlustur(GECERLI_DUZEN);

        yonetim(get(duzenAdresi), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(taslakId))
                .andExpect(jsonPath("$[0].seatCount").value(3));
    }

    @Test
    void shouldReturnNotFound_whenLayoutBelongsToAnotherHallOrHallMissing() throws Exception {
        long taslakId = taslakOlustur(GECERLI_DUZEN);
        String baskaSalonAdresi = "/api/v1/admin/catalog/halls/" + salonOlustur() + "/layouts/" + taslakId;

        yonetim(get(baskaSalonAdresi), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LAYOUT_NOT_FOUND"));
        yonetim(post("/api/v1/admin/catalog/halls/999999/layouts"), KUCUK_DUZEN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HALL_NOT_FOUND"));
    }

    @Test
    void shouldAssignVersionOneAndActivate_whenFirstDraftPublished() throws Exception {
        long taslakId = taslakOlustur(GECERLI_DUZEN);

        yonetim(post(duzenAdresi + "/" + taslakId + "/publish"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.cells", hasSize(6)));
    }

    @Test
    void shouldIncrementVersionAndMoveActiveFlag_whenSecondDraftPublished() throws Exception {
        long birinci = taslakOlustur(GECERLI_DUZEN);
        yonetim(post(duzenAdresi + "/" + birinci + "/publish"), null).andExpect(status().isOk());
        // Yayımdan sonra taslak kalmaz; yeni taslak açılabilir
        long ikinci = taslakOlustur(KUCUK_DUZEN);

        yonetim(post(duzenAdresi + "/" + ikinci + "/publish"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
        yonetim(get(duzenAdresi), null)
                .andExpect(jsonPath("$[0].id").value(ikinci))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].id").value(birinci))
                .andExpect(jsonPath("$[1].version").value(1))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    void shouldRejectChanges_whenLayoutAlreadyPublished() throws Exception {
        long taslakId = taslakOlustur(GECERLI_DUZEN);
        yonetim(post(duzenAdresi + "/" + taslakId + "/publish"), null).andExpect(status().isOk());

        yonetim(post(duzenAdresi + "/" + taslakId + "/publish"), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAYOUT_ALREADY_PUBLISHED"));
        yonetim(put(duzenAdresi + "/" + taslakId), KUCUK_DUZEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAYOUT_NOT_EDITABLE"));
        yonetim(get(duzenAdresi + "/" + taslakId), null)
                .andExpect(jsonPath("$.cells", hasSize(6)));
    }

    private long taslakOlustur(String govde) throws Exception {
        return idOku(yonetim(post(duzenAdresi), govde).andExpect(status().isCreated()));
    }

    private long salonOlustur() throws Exception {
        long subeId = idOku(yonetim(post("/api/v1/admin/catalog/cinemas"),
                "{\"name\":\"Şube %s\",\"city\":\"İstanbul\",\"address\":\"Adres\"}".formatted(UUID.randomUUID()))
                .andExpect(status().isCreated()));
        return idOku(yonetim(post("/api/v1/admin/catalog/halls"),
                "{\"cinemaId\":%d,\"name\":\"Salon 1\",\"defaultTicketPrice\":\"100\"}".formatted(subeId))
                .andExpect(status().isCreated()));
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
