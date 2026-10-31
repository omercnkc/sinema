package com.sinema.booking.bilet.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sinema.booking.rezervasyon.RezervasyonEntegrasyonTesti;
import com.sinema.booking.rezervasyon.service.OdemeAkisiServisi;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/** QR bilet doğrulama (K30): giriş istemez, yalnızca onaylı bilet geçerli, kişisel veri dönmez. */
class BiletDogrulamaKontrolcusuEntegrasyonTest extends RezervasyonEntegrasyonTesti {

    @Autowired
    private OdemeAkisiServisi odemeAkisiServisi;

    @Test
    void shouldShowTicketDetailsWithoutPersonalData_whenTicketConfirmed() throws Exception {
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), yeniSeansId(), "[8, 7]");
        String biletKodu = odemeAkisiServisi.onayla(rezervasyonId, 1L).ticketCode();

        dogrula(biletKodu)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.movieTitle").value("Örnek Film"))
                .andExpect(jsonPath("$.cinemaName").value("Kadıköy"))
                .andExpect(jsonPath("$.hallName").value("Salon 2"))
                .andExpect(jsonPath("$.seats", contains("A-7", "A-8")))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.totalAmount").doesNotExist())
                .andExpect(jsonPath("$.reservationId").doesNotExist());
    }

    @Test
    void shouldAcceptLowercaseCode_whenTypedByHand() throws Exception {
        long rezervasyonId = tutVeIdAl(yeniKullaniciId(), yeniSeansId(), "[1]");
        String biletKodu = odemeAkisiServisi.onayla(rezervasyonId, 1L).ticketCode();

        dogrula(biletKodu.toLowerCase(Locale.ROOT)).andExpect(status().isOk());
    }

    @Test
    void shouldReturnSameNotFound_whenCodeUnknownMalformedOrNotConfirmed() throws Exception {
        dogrula("ABCDEFGHJK234567")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
        dogrula("kisa").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
        dogrula("ILOU000000000000").andExpect(status().isNotFound()); // Crockford'da olmayan harfler

        long bekleyen = tutVeIdAl(yeniKullaniciId(), yeniSeansId(), "[1]");
        jdbcTemplate.update("UPDATE reservations SET ticket_code = 'BEKLEYEN00000000' WHERE id = ?", bekleyen);
        dogrula("BEKLEYEN00000000").andExpect(status().isNotFound());
    }

    private ResultActions dogrula(String kod) throws Exception {
        return mockMvc.perform(get("/api/v1/tickets/{kod}", kod));
    }
}
