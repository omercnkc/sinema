package com.sinema.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.testdestegi.TestTokenUretici;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Entegrasyon testleri için katalog verisi kurar (şube → salon → düzen → film → seans), admin API'si üzerinden:
 * test verisi de üretim kurallarından geçer. Adlar rastgele ekli: testler aynı DB'yi paylaşır, çakışmasınlar.
 */
public final class KatalogTestVerisi {

    public static final String ADMIN_TOKENI = "Bearer " + TestTokenUretici.kullaniciTokeni(1, "ADMIN");

    /** 2x3: üstte perde, altta A-1, A-2 koltukları ve bir duvar. */
    public static final String KUCUK_DUZEN = """
            {"gridRows":2,"gridCols":3,"cells":[
              {"row":0,"col":0,"type":"SCREEN"},{"row":0,"col":1,"type":"SCREEN"},
              {"row":1,"col":0,"type":"SEAT","seatRowLabel":"A","seatNumber":1,"seatType":"STANDARD"},
              {"row":1,"col":1,"type":"SEAT","seatRowLabel":"A","seatNumber":2,"seatType":"ACCESSIBLE"},
              {"row":1,"col":2,"type":"WALL"}]}""";

    private final MockMvc mockMvc;

    public KatalogTestVerisi(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public long subeOlustur() throws Exception {
        return idOku(yonetim(post("/api/v1/admin/catalog/cinemas"),
                "{\"name\":\"Şube %s\",\"city\":\"İstanbul\",\"address\":\"Adres\"}".formatted(UUID.randomUUID())));
    }

    public long salonOlustur(long subeId, String fiyat) throws Exception {
        return idOku(yonetim(post("/api/v1/admin/catalog/halls"),
                "{\"cinemaId\":%d,\"name\":\"Salon %s\",\"defaultTicketPrice\":\"%s\"}"
                        .formatted(subeId, UUID.randomUUID(), fiyat)));
    }

    /** Taslak oluşturup yayımlar; düzen id'sini döner. */
    public long duzenYayimla(long salonId, String duzenJson) throws Exception {
        String adres = "/api/v1/admin/catalog/halls/" + salonId + "/layouts";
        long taslak = idOku(yonetim(post(adres), duzenJson));
        yonetim(post(adres + "/" + taslak + "/publish"), null).andExpect(status().isOk());
        return taslak;
    }

    public long filmOlustur(int sureDakika) throws Exception {
        return idOku(yonetim(post("/api/v1/admin/catalog/movies"),
                "{\"title\":\"Film %s\",\"runtimeMinutes\":%d}".formatted(UUID.randomUUID(), sureDakika)));
    }

    public ResultActions seansOlusturIstegi(long filmId, long salonId, Instant baslangic, String fiyatJson)
            throws Exception {
        return yonetim(post("/api/v1/admin/catalog/showtimes"),
                "{\"movieId\":%d,\"hallId\":%d,\"startsAt\":\"%s\",\"ticketPrice\":%s}"
                        .formatted(filmId, salonId, baslangic, fiyatJson));
    }

    public long seansOlustur(long filmId, long salonId, Instant baslangic) throws Exception {
        return idOku(seansOlusturIstegi(filmId, salonId, baslangic, null));
    }

    public ResultActions yonetim(MockHttpServletRequestBuilder istek, String govde) throws Exception {
        istek.header("Authorization", ADMIN_TOKENI);
        if (govde != null) {
            istek.contentType(MediaType.APPLICATION_JSON).content(govde);
        }
        return mockMvc.perform(istek);
    }

    /** Oluşturma isteği 201 dönmeliydi; değilse test burada anlaşılır biçimde düşer. */
    public static long idOku(ResultActions sonuc) throws Exception {
        String govde = sonuc.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(govde, "$.id")).longValue();
    }
}
