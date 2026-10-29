package com.sinema.common.korelasyon;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class KorelasyonKimligiFiltresiTest {

    private final KorelasyonKimligiFiltresi filtre = new KorelasyonKimligiFiltresi();

    @Test
    void shouldKeepIncomingId_whenHeaderIsValid() throws Exception {
        // Given
        MockHttpServletRequest istek = new MockHttpServletRequest();
        istek.addHeader(KorelasyonKimligi.HTTP_BASLIGI, "abc-123");
        MockHttpServletResponse cevap = new MockHttpServletResponse();
        AtomicReference<String> zincirdekiMdc = new AtomicReference<>();
        FilterChain zincir = (i, c) -> zincirdekiMdc.set(MDC.get(KorelasyonKimligi.MDC_ANAHTARI));

        // When
        filtre.doFilter(istek, cevap, zincir);

        // Then
        assertThat(zincirdekiMdc.get()).isEqualTo("abc-123");
        assertThat(cevap.getHeader(KorelasyonKimligi.HTTP_BASLIGI)).isEqualTo("abc-123");
    }

    @Test
    void shouldGenerateNewId_whenHeaderIsMissing() throws Exception {
        MockHttpServletResponse cevap = new MockHttpServletResponse();

        filtre.doFilter(new MockHttpServletRequest(), cevap, (i, c) -> { });

        assertThat(cevap.getHeader(KorelasyonKimligi.HTTP_BASLIGI)).isNotBlank();
    }

    @Test
    void shouldReplaceId_whenHeaderContainsUnsafeCharacters() throws Exception {
        MockHttpServletRequest istek = new MockHttpServletRequest();
        istek.addHeader(KorelasyonKimligi.HTTP_BASLIGI, "kotu\nlog-satiri");
        MockHttpServletResponse cevap = new MockHttpServletResponse();

        filtre.doFilter(istek, cevap, (i, c) -> { });

        assertThat(cevap.getHeader(KorelasyonKimligi.HTTP_BASLIGI))
                .isNotEqualTo("kotu\nlog-satiri")
                .matches("[A-Za-z0-9-]+");
    }

    @Test
    void shouldClearMdc_afterRequestCompletes() throws Exception {
        filtre.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (i, c) -> { });

        assertThat(MDC.get(KorelasyonKimligi.MDC_ANAHTARI)).isNull();
    }
}
