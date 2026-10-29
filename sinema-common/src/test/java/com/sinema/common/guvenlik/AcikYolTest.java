package com.sinema.common.guvenlik;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class AcikYolTest {

    @Test
    void shouldParseMethodAndPattern_whenLineHasTwoParts() {
        assertThat(AcikYol.coz(" get   /api/v1/catalog/** ")).isEqualTo(new AcikYol(HttpMethod.GET, "/api/v1/catalog/**"));
    }

    @Test
    void shouldMatchAnyMethod_whenLineHasOnlyPattern() {
        assertThat(AcikYol.coz("/api/v1/tickets/**").metot()).isNull();
    }

    @Test
    void shouldFailFast_whenLineIsMalformed() {
        assertThatThrownBy(() -> AcikYol.coz("GET /a /b")).isInstanceOf(IllegalArgumentException.class);
    }
}
