package com.sinema.payment.odeme.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.payment.OdemeEntegrasyonTesti;
import com.sinema.payment.kullanici.istemci.AliciBilgisi;
import com.sinema.payment.saglayici.BaslatmaIstegi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class OdemeKontrolcusuEntegrasyonTest extends OdemeEntegrasyonTesti {

    private static final String YOL = "/api/v1/payments";

    @Test
    void shouldStartPaymentWithServerAmountAndExtendHold_whenOwnerPays() throws Exception {
        long rezervasyonId = yeniRezervasyonId();

        baslat(KULLANICI, rezervasyonId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value("300.00"))
                .andExpect(jsonPath("$.reservationId").value(rezervasyonId))
                .andExpect(jsonPath("$.paymentPageUrl").value(
                        startsWith("http://localhost:8080/api/v1/payments/mock/checkout/")));

        verify(rezervasyonIstemcisi).odemePenceresiniUzat(rezervasyonId);
        ArgumentCaptor<BaslatmaIstegi> istek = ArgumentCaptor.forClass(BaslatmaIstegi.class);
        verify(saglayici).baslat(istek.capture());
        assertThat(istek.getValue().tutar()).isEqualByComparingTo(TUTAR);
        assertThat(istek.getValue().sepetKimligi()).isEqualTo(String.valueOf(rezervasyonId));
        assertThat(istek.getValue().alici().ad()).isEqualTo("Ayşe");
        assertThat(istek.getValue().alici().telefon()).isEqualTo("+905551112233");
        assertThat(istek.getValue().alici().kimlikNo()).isEqualTo("11111111111");
        assertThat(istek.getValue().callbackAdresi()).hasToString("http://localhost:8080/api/v1/payments/callback/iyzico");
    }

    @Test
    void shouldReturnSamePaymentAndPage_whenPayRequestedAgain() throws Exception {
        long rezervasyonId = yeniRezervasyonId();
        String ilk = baslat(KULLANICI, rezervasyonId).andReturn().getResponse().getContentAsString();

        String ikinci = baslat(KULLANICI, rezervasyonId).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat((Object) JsonPath.read(ikinci, "$.paymentId")).isEqualTo(JsonPath.read(ilk, "$.paymentId"));
        assertThat((String) JsonPath.read(ikinci, "$.paymentPageUrl")).isEqualTo(JsonPath.read(ilk, "$.paymentPageUrl"));
        verify(saglayici, times(1)).baslat(any());
    }

    @Test
    void shouldHideOtherUsersReservation_andNotTouchHold() throws Exception {
        long rezervasyonId = yeniRezervasyonId();

        baslat(KULLANICI + 1, rezervasyonId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));

        verify(rezervasyonIstemcisi, never()).odemePenceresiniUzat(anyLong());
    }

    @Test
    void shouldRejectWithoutStartingProvider_whenReservationNoLongerPayable() throws Exception {
        long rezervasyonId = yeniRezervasyonId();
        when(rezervasyonIstemcisi.odemeBilgisi(rezervasyonId)).thenReturn(rezervasyon(rezervasyonId, KULLANICI,
                "EXPIRED"));

        baslat(KULLANICI, rezervasyonId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_PAYABLE"));
        verify(saglayici, never()).baslat(any());
    }

    @Test
    void shouldPassBookingConflict_whenHoldExpiresWhileExtending() throws Exception {
        long rezervasyonId = yeniRezervasyonId();
        when(rezervasyonIstemcisi.odemePenceresiniUzat(rezervasyonId))
                .thenThrow(new CakismaIstisnasi("RESERVATION_NOT_ACTIVE", "Rezervasyonun süresi dolmuş."));

        baslat(KULLANICI, rezervasyonId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_ACTIVE"));
    }

    @Test
    void shouldUseFallbackBuyerFields_whenAccountHasNoPersonalInfo() throws Exception {
        long rezervasyonId = yeniRezervasyonId();
        when(kullaniciIstemcisi.aliciBilgisi(KULLANICI))
                .thenReturn(new AliciBilgisi(KULLANICI, "admin@sinema.local", null, null, null));

        baslat(KULLANICI, rezervasyonId).andExpect(status().isCreated());

        ArgumentCaptor<BaslatmaIstegi> istek = ArgumentCaptor.forClass(BaslatmaIstegi.class);
        verify(saglayici).baslat(istek.capture());
        assertThat(istek.getValue().alici().ad()).isEqualTo("Sinema");
        assertThat(istek.getValue().alici().telefon()).isEqualTo("+905350000000");
        assertThat(istek.getValue().alici().email()).isEqualTo("admin@sinema.local");
    }

    @Test
    void shouldReturnServiceUnavailable_andRetryWithSameRecord_whenAuthDownThenUp() throws Exception {
        long rezervasyonId = yeniRezervasyonId();
        when(kullaniciIstemcisi.aliciBilgisi(KULLANICI))
                .thenThrow(new BagimlilikKullanilamazIstisnasi("AUTH_UNAVAILABLE", "x"))
                .thenReturn(new AliciBilgisi(KULLANICI, "a@b.com", "A", "B", "+905551112233"));

        baslat(KULLANICI, rezervasyonId).andExpect(status().isServiceUnavailable());
        baslat(KULLANICI, rezervasyonId).andExpect(status().isCreated());
    }

    @Test
    void shouldShowPaymentOnlyToOwner_whenQueriedByReservation() throws Exception {
        long rezervasyonId = yeniRezervasyonId();
        baslat(KULLANICI, rezervasyonId);

        mockMvc.perform(get(YOL + "/by-reservation/{id}", rezervasyonId).header("Authorization", yetki(KULLANICI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        mockMvc.perform(get(YOL + "/by-reservation/{id}", rezervasyonId).header("Authorization", yetki(KULLANICI + 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRequireLoginAndValidBody() throws Exception {
        mockMvc.perform(post(YOL).contentType(MediaType.APPLICATION_JSON).content("{\"reservationId\":1}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(YOL).header("Authorization", yetki(KULLANICI)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":1,\"amount\":\"1.00\"}"))
                .andExpect(status().isCreated()); // tutar alanı yok sayılır, sunucu tutarı kullanılır
        mockMvc.perform(post(YOL).header("Authorization", yetki(KULLANICI)).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions baslat(long kullaniciId, long rezervasyonId) throws Exception {
        return mockMvc.perform(post(YOL).header("Authorization", yetki(kullaniciId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservationId\":" + rezervasyonId + "}"));
    }
}
