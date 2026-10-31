package com.sinema.auth.kullanici.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.auth.kullanici.dto.AdminIstegi;
import com.sinema.auth.kullanici.dto.KayitIstegi;
import com.sinema.auth.kullanici.service.KullaniciKayitServisi;
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

/** K48: payment'ın iyzico alıcı bilgisini okuduğu iç uç; yalnızca auth'a kesilmiş SERVICE token'ı ile. */
@SpringBootTest(properties = "sinema.auth.servis-istemcileri.payment-service.sir="
        + KullaniciIcKontrolcusuEntegrasyonTest.PAYMENT_SIRRI)
@AutoConfigureMockMvc
@Import(MysqlTestYapilandirmasi.class)
class KullaniciIcKontrolcusuEntegrasyonTest {

    static final String PAYMENT_SIRRI = "payment-test-sirri";
    private static final String YOL = "/internal/v1/auth/users/{id}/billing-info";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KullaniciKayitServisi kayitServisi;

    @Test
    void shouldReturnBuyerInfo_whenPaymentUsesServiceTokenObtainedFromAuth() throws Exception {
        String email = benzersizEmail();
        Long kullaniciId = kayitServisi.kaydet(new KayitIstegi(email, "Parola123", "Ayşe", "Demir", "05551112233"))
                .id();

        aliciBilgisi(kullaniciId, paymentTokeniAl())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(kullaniciId))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Ayşe"))
                .andExpect(jsonPath("$.lastName").value("Demir"))
                .andExpect(jsonPath("$.phone").value("+905551112233"));
    }

    @Test
    void shouldReturnEmptyPersonalFields_whenAccountHasNoPersonalInfo() throws Exception {
        Long adminId = kayitServisi.adminEkle(new AdminIstegi(benzersizEmail(), "Parola123")).id();

        aliciBilgisi(adminId, TestTokenUretici.servisTokeni("payment-service", "auth-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void shouldReturnNotFound_whenUserMissing() throws Exception {
        aliciBilgisi(Long.MAX_VALUE, TestTokenUretici.servisTokeni("payment-service", "auth-service"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldForbid_whenUserTokenOrTokenForAnotherService() throws Exception {
        aliciBilgisi(1L, TestTokenUretici.kullaniciTokeni(1, "USER")).andExpect(status().isForbidden());
        aliciBilgisi(1L, TestTokenUretici.servisTokeni("payment-service", "booking-service"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(YOL, 1L)).andExpect(status().isUnauthorized());
    }

    private String paymentTokeniAl() throws Exception {
        String govde = mockMvc.perform(post("/internal/v1/auth/service-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"payment-service\",\"clientSecret\":\"" + PAYMENT_SIRRI
                                + "\",\"audience\":\"auth-service\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(govde, "$.accessToken");
    }

    private ResultActions aliciBilgisi(Long kullaniciId, String token) throws Exception {
        return mockMvc.perform(get(YOL, kullaniciId).header("Authorization", "Bearer " + token));
    }

    private static String benzersizEmail() {
        return "alici-" + UUID.randomUUID() + "@ornek.com";
    }
}
