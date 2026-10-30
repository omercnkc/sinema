package com.sinema.common.servisarasi;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuvenliYontemYenidenDeneyiciTest {

    // Testi bekletmemek için 1 ms bekleme; 3 deneme = ilk + 2 tekrar
    private final Retryer deneyici = new GuvenliYontemYenidenDeneyici(new Retryer.Default(1, 1, 3));

    @Test
    void shouldRetryUpToLimit_whenGetRequestFailsWithNetworkError() {
        Retryer cagriyaOzel = deneyici.clone();
        RetryableException hata = agHatasi(Request.HttpMethod.GET);

        assertThatCode(() -> cagriyaOzel.continueOrPropagate(hata)).doesNotThrowAnyException();
        assertThatCode(() -> cagriyaOzel.continueOrPropagate(hata)).doesNotThrowAnyException();
        assertThatThrownBy(() -> cagriyaOzel.continueOrPropagate(hata)).isSameAs(hata);
    }

    @Test
    void shouldNotRetry_whenPostRequestFails() {
        RetryableException hata = agHatasi(Request.HttpMethod.POST);

        assertThatThrownBy(() -> deneyici.clone().continueOrPropagate(hata)).isSameAs(hata);
    }

    @Test
    void shouldStartWithFreshCounter_whenClonedForNewCall() {
        RetryableException hata = agHatasi(Request.HttpMethod.GET);
        Retryer ilkCagri = deneyici.clone();
        ilkCagri.continueOrPropagate(hata);
        ilkCagri.continueOrPropagate(hata);

        assertThatCode(() -> deneyici.clone().continueOrPropagate(hata)).doesNotThrowAnyException();
    }

    private static RetryableException agHatasi(Request.HttpMethod metot) {
        Request istek = Request.create(metot, "http://catalog-service/x", Map.of(), null, StandardCharsets.UTF_8, null);
        return new RetryableException(-1, "Read timed out", metot, (Long) null, istek);
    }
}
