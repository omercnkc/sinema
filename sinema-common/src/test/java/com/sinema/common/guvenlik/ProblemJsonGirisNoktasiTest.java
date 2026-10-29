package com.sinema.common.guvenlik;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class ProblemJsonGirisNoktasiTest {

    private final ProblemJsonYazici yazici = new ProblemJsonYazici();

    @Test
    void shouldWriteUnauthorizedProblemJson_whenAuthenticationFails() throws Exception {
        MockHttpServletResponse cevap = new MockHttpServletResponse();

        new ProblemJsonGirisNoktasi(yazici).commence(new MockHttpServletRequest("GET", "/api/v1/auth/me"), cevap,
                new BadCredentialsException("imza bozuk"));

        assertThat(cevap.getStatus()).isEqualTo(401);
        assertThat(cevap.getContentType()).startsWith("application/problem+json");
        assertThat(cevap.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(cevap.getContentAsString())
                .contains("\"code\":\"UNAUTHORIZED\"")
                .contains("\"correlationId\"")
                .contains("\"instance\":\"/api/v1/auth/me\"")
                .doesNotContain("\"type\"")
                .doesNotContain("imza bozuk")
                .doesNotContain("properties");
    }

    @Test
    void shouldWriteForbiddenProblemJson_whenAccessDenied() throws Exception {
        MockHttpServletResponse cevap = new MockHttpServletResponse();

        new ProblemJsonErisimReddiIsleyicisi(yazici).handle(new MockHttpServletRequest(), cevap,
                new AccessDeniedException("rol yetersiz"));

        assertThat(cevap.getStatus()).isEqualTo(403);
        assertThat(cevap.getContentAsString()).contains("\"code\":\"FORBIDDEN\"");
    }
}
