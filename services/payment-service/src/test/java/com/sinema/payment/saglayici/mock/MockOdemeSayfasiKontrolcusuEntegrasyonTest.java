package com.sinema.payment.saglayici.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sinema.payment.OdemeEntegrasyonTesti;
import com.sinema.payment.saglayici.SorguDurumu;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Sahte ödeme sayfası (K49): giriş istemez, seçimi kaydeder ve tarayıcıyı iyzico gibi callback'e gönderir. */
class MockOdemeSayfasiKontrolcusuEntegrasyonTest extends OdemeEntegrasyonTesti {

    @Test
    void shouldShowAmountAndChoices_withoutLogin() throws Exception {
        String token = odemeBaslat().token();

        mockMvc.perform(get(sayfa(token)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("300.00 TL")))
                .andExpect(content().string(containsString(token + "/complete")));
    }

    @Test
    void shouldRecordChoiceAndAutoPostTokenToCallback_whenUserCompletes() throws Exception {
        String token = odemeBaslat().token();

        mockMvc.perform(post(sayfa(token) + "/complete").param("sonuc", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "action=\"http://localhost:8080/api/v1/payments/callback/iyzico\"")))
                .andExpect(content().string(containsString("name=\"token\" value=\"" + token + "\"")));

        assertThat(saglayici.sonucuSorgula(token, "x").durum()).isEqualTo(SorguDurumu.BASARILI);
    }

    @Test
    void shouldReturnNotFound_whenTokenUnknownOrMalformed() throws Exception {
        mockMvc.perform(get(sayfa("bilinmeyen-token"))).andExpect(status().isNotFound());
        mockMvc.perform(get(sayfa("<script>"))).andExpect(status().isNotFound());
        mockMvc.perform(post(sayfa("bilinmeyen-token") + "/complete").param("sonuc", "SUCCESS"))
                .andExpect(status().isNotFound());
    }

    private static String sayfa(String token) {
        return "/api/v1/payments/mock/checkout/" + token;
    }
}
