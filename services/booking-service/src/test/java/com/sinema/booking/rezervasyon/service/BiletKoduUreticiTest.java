package com.sinema.booking.rezervasyon.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BiletKoduUreticiTest {

    private final BiletKoduUretici uretici = new BiletKoduUretici();

    @Test
    void shouldProduceSixteenCharCodesWithoutAmbiguousLetters() {
        for (int i = 0; i < 1_000; i++) {
            assertThat(uretici.uret()).hasSize(BiletKoduUretici.UZUNLUK).matches("[0-9A-HJKMNP-TV-Z]+");
        }
    }

    @Test
    void shouldNotRepeatCodes_whenManyGenerated() {
        Set<String> kodlar = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            kodlar.add(uretici.uret());
        }
        assertThat(kodlar).hasSize(10_000);
    }
}
