package com.sinema.auth.oturum.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.auth.AyarlanabilirSaat;
import com.sinema.auth.kullanici.dto.KayitIstegi;
import com.sinema.auth.kullanici.service.KullaniciKayitServisi;
import com.sinema.auth.oturum.service.GirisServisi;
import com.sinema.auth.oturum.service.YenilemeServisi;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import({MysqlTestYapilandirmasi.class, OturumKontrolcusuEntegrasyonTest.SaatYapilandirmasi.class})
class OturumKontrolcusuEntegrasyonTest {

    private static final String GIRIS_ADRESI = "/api/v1/auth/login";
    private static final String YENILEME_ADRESI = "/api/v1/auth/refresh";
    private static final String CEREZ_ADI = "sinema_refresh";
    private static final String DOGRU_PAROLA = "Parola123";
    private static final String YANLIS_PAROLA = "Yanlis123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KullaniciKayitServisi kayitServisi;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private AyarlanabilirSaat saat;

    @AfterEach
    void saatiSifirla() {
        saat.sifirla();
    }

    @Test
    void shouldReturnSignedTokenWithExpectedClaims_whenCredentialsAreValid() throws Exception {
        String email = yeniKullanici();
        Long kullaniciId = kayitServisi.kaydet(new KayitIstegi(email + ".x", DOGRU_PAROLA)).id();

        String govde = girisYap(email + ".x", DOGRU_PAROLA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andReturn().getResponse().getContentAsString();

        Jwt token = jwtDecoder.decode(JsonPath.read(govde, "$.accessToken"));
        assertThat(token.getSubject()).isEqualTo(kullaniciId.toString());
        assertThat(token.getClaimAsString("role")).isEqualTo("USER");
        // getIssuer() değeri URL'e çevirmeye çalışır; bizim "iss" düz bir addır
        assertThat(token.getClaimAsString("iss")).isEqualTo("sinema-auth");
        assertThat(token.getId()).isNotBlank();
        assertThat(token.getHeaders()).containsKey("kid").containsEntry("alg", "RS256");
        assertThat(Duration.between(token.getIssuedAt(), token.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(token.getClaims()).doesNotContainKey("email");
    }

    @Test
    void shouldReturnSameGenericError_whenPasswordWrongOrEmailUnknown() throws Exception {
        String email = yeniKullanici();

        String yanlisParola = girisYap(email, YANLIS_PAROLA).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String bilinmeyenEmail = girisYap("yok-" + email, DOGRU_PAROLA).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(yanlisParola, "$.detail"))
                .isEqualTo(JsonPath.read(bilinmeyenEmail, "$.detail"));
        assertThat((String) JsonPath.read(yanlisParola, "$.code")).isEqualTo(GirisServisi.GECERSIZ_KIMLIK_BILGISI_KODU);
    }

    @Test
    void shouldLockAfterFiveFailuresAndUnlockAfterFiveMinutes_whenPasswordKeepsFailing() throws Exception {
        String email = yeniKullanici();
        for (int deneme = 1; deneme <= 5; deneme++) {
            girisYap(email, YANLIS_PAROLA).andExpect(status().isUnauthorized());
        }

        // Kilitliyken doğru parola da reddedilir
        girisYap(email, DOGRU_PAROLA)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "300"))
                .andExpect(jsonPath("$.code").value(GirisServisi.HESAP_KILITLI_KODU));

        saat.ilerlet(Duration.ofMinutes(5).plusSeconds(1));
        girisYap(email, DOGRU_PAROLA).andExpect(status().isOk());
    }

    @Test
    void shouldResetFailureCount_whenLoginSucceeds() throws Exception {
        String email = yeniKullanici();
        for (int deneme = 1; deneme <= 4; deneme++) {
            girisYap(email, YANLIS_PAROLA).andExpect(status().isUnauthorized());
        }
        girisYap(email, DOGRU_PAROLA).andExpect(status().isOk());

        for (int deneme = 1; deneme <= 4; deneme++) {
            girisYap(email, YANLIS_PAROLA).andExpect(status().isUnauthorized());
        }
        girisYap(email, DOGRU_PAROLA).andExpect(status().isOk());
    }

    @Test
    void shouldLockAccount_whenFailedAttemptsArriveConcurrently() throws Exception {
        String email = yeniKullanici();
        int esZamanliDeneme = 8;

        try (ExecutorService havuz = Executors.newFixedThreadPool(esZamanliDeneme)) {
            var gorevler = IntStream.range(0, esZamanliDeneme)
                    .<Callable<Integer>>mapToObj(i -> () ->
                            girisYap(email, YANLIS_PAROLA).andReturn().getResponse().getStatus())
                    .toList();
            for (var sonuc : havuz.invokeAll(gorevler)) {
                assertThat(sonuc.get()).isIn(401, 429);
            }
        }

        // Sayaç artışları kaybolsaydı hesap kilitlenmeyebilirdi
        girisYap(email, DOGRU_PAROLA).andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldReturnUnauthorizedProblemJson_whenProtectedPathCalledWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ---------- refresh token (alt görev 4) ----------

    @Test
    void shouldSetHardenedRefreshCookie_whenLoginSucceeds() throws Exception {
        String setCookie = girisYap(yeniKullanici(), DOGRU_PAROLA).andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Set-Cookie");

        assertThat(setCookie).startsWith(CEREZ_ADI + "=")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth", "Max-Age=1209600");
    }

    @Test
    void shouldRotateRefreshTokenAndReturnNewAccessToken_whenCookieIsValid() throws Exception {
        Cookie ilk = refreshCerezi(girisYap(yeniKullanici(), DOGRU_PAROLA));

        Cookie ikinci = refreshCerezi(yenile(ilk)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty()));

        assertThat(ikinci.getValue()).isNotEqualTo(ilk.getValue());
        yenile(ikinci).andExpect(status().isOk());
    }

    @Test
    void shouldGiveAccessTokenWithoutRotating_whenOldCookieReusedWithinGracePeriod() throws Exception {
        Cookie ilk = refreshCerezi(girisYap(yeniKullanici(), DOGRU_PAROLA));
        yenile(ilk).andExpect(status().isOk());

        // İkinci sekme aynı eski cookie ile hemen geldi: access token alır, cookie değişmez
        yenile(ilk)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void shouldRevokeWholeFamily_whenOldCookieReusedAfterGracePeriod() throws Exception {
        Cookie calinan = refreshCerezi(girisYap(yeniKullanici(), DOGRU_PAROLA));
        Cookie gercekKullanicininki = refreshCerezi(yenile(calinan));

        saat.ilerlet(Duration.ofSeconds(11));
        yenile(calinan)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(YenilemeServisi.YENIDEN_KULLANIM_KODU));

        // Aile iptal edildi: gerçek kullanıcının güncel token'ı da artık geçmez (yeniden giriş gerekir)
        yenile(gercekKullanicininki).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRefresh_whenCookieMissingOrExpired() throws Exception {
        mockMvc.perform(post(YENILEME_ADRESI))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(YenilemeServisi.GECERSIZ_YENILEME_KODU));

        Cookie cerez = refreshCerezi(girisYap(yeniKullanici(), DOGRU_PAROLA));
        saat.ilerlet(Duration.ofDays(14).plusSeconds(1));
        yenile(cerez).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldClearCookieAndRevokeToken_whenUserLogsOut() throws Exception {
        Cookie cerez = refreshCerezi(girisYap(yeniKullanici(), DOGRU_PAROLA));

        String setCookie = mockMvc.perform(post("/api/v1/auth/logout").cookie(cerez))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getHeader("Set-Cookie");

        assertThat(setCookie).startsWith(CEREZ_ADI + "=;").contains("Max-Age=0");
        yenile(cerez)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(YenilemeServisi.GECERSIZ_YENILEME_KODU));
    }

    @Test
    void shouldReturnOwnProfile_whenAccessTokenIsValid() throws Exception {
        String email = yeniKullanici();
        String govde = girisYap(email, DOGRU_PAROLA).andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + JsonPath.read(govde, "$.accessToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    private ResultActions yenile(Cookie cerez) throws Exception {
        return mockMvc.perform(post(YENILEME_ADRESI).cookie(cerez));
    }

    private static Cookie refreshCerezi(ResultActions sonuc) {
        Cookie cerez = sonuc.andReturn().getResponse().getCookie(CEREZ_ADI);
        assertThat(cerez).as("Set-Cookie ile refresh cookie dönmeli").isNotNull();
        return cerez;
    }

    private String yeniKullanici() {
        String email = "giris-" + UUID.randomUUID() + "@ornek.com";
        kayitServisi.kaydet(new KayitIstegi(email, DOGRU_PAROLA));
        return email;
    }

    private ResultActions girisYap(String email, String parola) throws Exception {
        String govde = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, parola);
        return mockMvc.perform(post(GIRIS_ADRESI).contentType(MediaType.APPLICATION_JSON).content(govde));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SaatYapilandirmasi {

        @Bean
        @Primary
        AyarlanabilirSaat ayarlanabilirSaat() {
            return new AyarlanabilirSaat();
        }
    }
}
