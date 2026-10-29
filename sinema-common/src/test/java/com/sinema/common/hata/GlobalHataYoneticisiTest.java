package com.sinema.common.hata;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sinema.common.korelasyon.KorelasyonKimligi;
import com.sinema.common.korelasyon.KorelasyonKimligiFiltresi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalHataYoneticisiTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DenemeKontrolcusu())
            .setControllerAdvice(new GlobalHataYoneticisi())
            .addFilters(new KorelasyonKimligiFiltresi())
            .build();

    @Test
    void shouldReturnProblemWithCodeAndExtraFields_whenConflictExceptionThrown() throws Exception {
        mockMvc.perform(get("/cakisma").header(KorelasyonKimligi.HTTP_BASLIGI, "kimlik-1"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("SEATS_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value("Seçilen koltuklar az önce alındı."))
                .andExpect(jsonPath("$.unavailableCellIds[0]").value(7))
                .andExpect(jsonPath("$.correlationId").value("kimlik-1"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    void shouldReturnNotFoundProblem_whenResourceMissing() throws Exception {
        mockMvc.perform(get("/yok"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void shouldListFieldErrors_whenValidationFails() throws Exception {
        mockMvc.perform(post("/kayit").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"gecersiz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(GlobalHataYoneticisi.DOGRULAMA_HATASI_KODU))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value(notNullValue()))
                .andExpect(jsonPath("$.correlationId").value(notNullValue()));
    }

    @Test
    void shouldUseStatusNameAsCode_whenSpringRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/kayit").contentType(MediaType.APPLICATION_JSON).content("{bozuk"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.correlationId").value(notNullValue()));
    }

    @Test
    void shouldSetRetryAfterHeaderRoundedUp_whenTooManyRequestsThrown() throws Exception {
        mockMvc.perform(get("/bekle"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "91"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(91))
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void shouldHideInternalDetails_whenUnexpectedExceptionThrown() throws Exception {
        mockMvc.perform(get("/patla"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(GlobalHataYoneticisi.BEKLENMEYEN_HATA_KODU))
                .andExpect(content().string(not(containsString("gizli-sql-ayrintisi"))))
                .andExpect(header().exists(KorelasyonKimligi.HTTP_BASLIGI));
    }

    @RestController
    static class DenemeKontrolcusu {

        @GetMapping("/cakisma")
        void cakisma() {
            throw new CakismaIstisnasi("SEATS_UNAVAILABLE", "Seçilen koltuklar az önce alındı.",
                    Map.of("unavailableCellIds", List.of(7)));
        }

        @GetMapping("/yok")
        void yok() {
            throw new KaynakBulunamadiIstisnasi("RESERVATION_NOT_FOUND", "Rezervasyon bulunamadı.");
        }

        @PostMapping("/kayit")
        void kayit(@Valid @RequestBody DenemeIstegi istek) {
        }

        @GetMapping("/bekle")
        void bekle() {
            throw new CokFazlaIstekIstisnasi("ACCOUNT_LOCKED", "Hesap kilitli.", Duration.ofMillis(90_001));
        }

        @GetMapping("/patla")
        void patla() {
            throw new IllegalStateException("gizli-sql-ayrintisi");
        }
    }

    record DenemeIstegi(@NotBlank @Email String email) {
    }
}
