package com.sinema.auth.kullanici.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TelefonNumarasiTest {

    @ParameterizedTest
    @ValueSource(strings = {"05321234567", "5321234567", "+905321234567", "905321234567", "00905321234567",
            "0532 123 45 67", "(0532) 123-45-67", " +90 532 123 4567 ", "0532.123.45.67"})
    void shouldNormalizeToE164_whenTurkishMobileWrittenInCommonFormats(String girilen) {
        assertThat(TelefonNumarasi.normalizeEt(girilen)).hasValue("+905321234567");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"02121234567", "532123456", "53212345678", "+4915112345678", "0532abc4567",
            "+90 532 123 456 78", "telefon"})
    void shouldReject_whenNotTurkishMobileNumber(String girilen) {
        assertThat(TelefonNumarasi.normalizeEt(girilen)).isEmpty();
    }
}
