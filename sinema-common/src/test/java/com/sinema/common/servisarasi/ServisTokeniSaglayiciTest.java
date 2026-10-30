package com.sinema.common.servisarasi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

class ServisTokeniSaglayiciTest {

    private static final String AUTH_ADRESI = "http://auth";
    private static final String TOKEN_ADRESI = AUTH_ADRESI + ServisTokeniSaglayici.TOKEN_YOLU;
    private static final Duration YENILEME_PAYI = Duration.ofSeconds(30);
    private static final Instant BASLANGIC = Instant.parse("2026-10-30T10:00:00Z");

    private MockRestServiceServer sahteAuth;
    private AyarlanabilirSaat saat;
    private ServisTokeniSaglayici saglayici;

    @BeforeEach
    void kur() {
        RestClient.Builder kurucu = RestClient.builder().baseUrl(AUTH_ADRESI);
        sahteAuth = MockRestServiceServer.bindTo(kurucu).build();
        saat = new AyarlanabilirSaat(BASLANGIC);
        saglayici = new ServisTokeniSaglayici(kurucu.build(), "booking-service", "gizli-sir", YENILEME_PAYI, saat);
    }

    @Test
    void shouldRequestTokenWithClientCredentialsAndAudience_whenNoCachedToken() {
        sahteAuth.expect(once(), requestTo(TOKEN_ADRESI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"clientId":"booking-service","clientSecret":"gizli-sir","audience":"catalog-service"}"""))
                .andRespond(tokenCevabi("token-1", 300));

        assertThat(saglayici.token("catalog-service")).isEqualTo("token-1");
        sahteAuth.verify();
    }

    @Test
    void shouldReuseCachedToken_whenTokenStillValid() {
        sahteAuth.expect(once(), requestTo(TOKEN_ADRESI)).andRespond(tokenCevabi("token-1", 300));

        saglayici.token("catalog-service");
        saat.ilerlet(Duration.ofSeconds(269));

        assertThat(saglayici.token("catalog-service")).isEqualTo("token-1");
        sahteAuth.verify();
    }

    @Test
    void shouldRenewToken_whenWithinRenewalMarginOfExpiry() {
        sahteAuth.expect(once(), requestTo(TOKEN_ADRESI)).andRespond(tokenCevabi("token-1", 300));
        sahteAuth.expect(once(), requestTo(TOKEN_ADRESI)).andRespond(tokenCevabi("token-2", 300));

        saglayici.token("catalog-service");
        saat.ilerlet(Duration.ofSeconds(270)); // 300 - 30 sn pay: artık yenilenmeli

        assertThat(saglayici.token("catalog-service")).isEqualTo("token-2");
        sahteAuth.verify();
    }

    @Test
    void shouldKeepSeparateTokens_whenTargetsDiffer() {
        sahteAuth.expect(once(), requestTo(TOKEN_ADRESI))
                .andExpect(content().json("{\"audience\":\"catalog-service\"}"))
                .andRespond(tokenCevabi("katalog-tokeni", 300));
        sahteAuth.expect(once(), requestTo(TOKEN_ADRESI))
                .andExpect(content().json("{\"audience\":\"booking-service\"}"))
                .andRespond(tokenCevabi("rezervasyon-tokeni", 300));

        assertThat(saglayici.token("catalog-service")).isEqualTo("katalog-tokeni");
        assertThat(saglayici.token("booking-service")).isEqualTo("rezervasyon-tokeni");
    }

    @Test
    void shouldThrowServiceUnavailable_whenAuthRejectsCredentials() {
        sahteAuth.expect(once(), requestTo(TOKEN_ADRESI)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> saglayici.token("catalog-service"))
                .isInstanceOf(BagimlilikKullanilamazIstisnasi.class)
                .hasFieldOrPropertyWithValue("kod", "SERVICE_AUTH_UNAVAILABLE");
    }

    @Test
    void shouldNotExposeSecret_whenRequestPrinted() {
        assertThat(new ServisTokeniSaglayici.ServisTokeniIstegi("booking-service", "gizli-sir", "catalog-service"))
                .asString().doesNotContain("gizli-sir");
    }

    private static ResponseCreator tokenCevabi(String token, long sureSaniye) {
        return withSuccess("{\"accessToken\":\"" + token + "\",\"expiresInSeconds\":" + sureSaniye + "}",
                MediaType.APPLICATION_JSON);
    }

    /** Süreye bağlı davranışı beklemeden test etmek için elle ilerletilen saat. */
    private static final class AyarlanabilirSaat extends Clock {

        private Instant simdi;

        AyarlanabilirSaat(Instant baslangic) {
            this.simdi = baslangic;
        }

        void ilerlet(Duration sure) {
            simdi = simdi.plus(sure);
        }

        @Override
        public Instant instant() {
            return simdi;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
