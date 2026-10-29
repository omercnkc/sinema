package com.sinema.auth.servis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.auth.servis.service.ServisTokeniServisi;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// payment-service'in sırrı bilerek tanımsız: sırrı boş istemci devre dışı olmalı
@SpringBootTest(properties = "sinema.auth.servis-istemcileri.booking-service.sir=" + ServisTokeniKontrolcusuEntegrasyonTest.BOOKING_SIRRI)
@AutoConfigureMockMvc
@Import(MysqlTestYapilandirmasi.class)
class ServisTokeniKontrolcusuEntegrasyonTest {

    static final String BOOKING_SIRRI = "booking-test-sirri-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void shouldIssueShortLivedServiceToken_whenCredentialsAndAudienceAreValid() throws Exception {
        String govde = tokenIste("booking-service", BOOKING_SIRRI, "catalog-service")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(300))
                .andReturn().getResponse().getContentAsString();

        Jwt token = jwtDecoder.decode(JsonPath.read(govde, "$.accessToken"));
        assertThat(token.getSubject()).isEqualTo("booking-service");
        assertThat(token.getClaimAsString("role")).isEqualTo("SERVICE");
        assertThat(token.getAudience()).containsExactly("catalog-service");
        assertThat(Duration.between(token.getIssuedAt(), token.getExpiresAt())).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldRejectWithSameError_whenSecretWrongOrClientUnknownOrDisabled() throws Exception {
        tokenIste("booking-service", "yanlis-sir", "catalog-service")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ServisTokeniServisi.GECERSIZ_ISTEMCI_KODU));
        tokenIste("bilinmeyen-servis", BOOKING_SIRRI, "catalog-service").andExpect(status().isUnauthorized());
        tokenIste("payment-service", "", "booking-service").andExpect(status().isBadRequest());
        tokenIste("payment-service", "herhangi", "booking-service").andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbid_whenClientAsksForAudienceOutsideItsAllowList() throws Exception {
        // booking yalnızca catalog'a gidebilir; payment'a (bağımlılık yönünün tersine) token alamaz
        tokenIste("booking-service", BOOKING_SIRRI, "payment-service")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ServisTokeniServisi.IZINSIZ_HEDEF_KODU));
    }

    @Test
    void shouldForbidServiceToken_whenUsedOnUserEndpoint() throws Exception {
        String govde = tokenIste("booking-service", BOOKING_SIRRI, "catalog-service")
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + JsonPath.read(govde, "$.accessToken")))
                .andExpect(status().isForbidden());
    }

    private ResultActions tokenIste(String istemci, String sir, String hedef) throws Exception {
        String govde = "{\"clientId\":\"%s\",\"clientSecret\":\"%s\",\"audience\":\"%s\"}".formatted(istemci, sir, hedef);
        return mockMvc.perform(post("/internal/v1/auth/service-token")
                .contentType(MediaType.APPLICATION_JSON).content(govde));
    }
}
