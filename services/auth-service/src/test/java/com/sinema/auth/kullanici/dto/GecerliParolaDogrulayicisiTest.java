package com.sinema.auth.kullanici.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GecerliParolaDogrulayicisiTest {

    private final GecerliParolaDogrulayicisi dogrulayici = new GecerliParolaDogrulayicisi();

    @ParameterizedTest
    @ValueSource(strings = {"Parola12", "çok güçlü bir parola 2026"})
    void shouldAccept_whenPasswordFitsPolicy(String parola) {
        assertThat(dogrulayici.isValid(parola, null)).isTrue();
    }

    @Test
    void shouldReject_whenPasswordIsShorterThanEightCharacters() {
        assertThat(dogrulayici.isValid("Kisa123", null)).isFalse();
    }

    @Test
    void shouldReject_whenPasswordIsNull() {
        assertThat(dogrulayici.isValid(null, null)).isFalse();
    }

    @Test
    void shouldReject_whenTurkishCharactersExceed72BytesEvenUnder72Characters() {
        String parola = "ğ".repeat(37); // 37 karakter ama UTF-8'de 74 bayt

        assertThat(parola).hasSizeLessThan(72);
        assertThat(dogrulayici.isValid(parola, null)).isFalse();
    }
}
