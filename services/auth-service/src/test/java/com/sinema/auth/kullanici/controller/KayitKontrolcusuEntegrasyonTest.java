package com.sinema.auth.kullanici.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sinema.auth.kullanici.entity.Kullanici;
import com.sinema.auth.kullanici.entity.Rol;
import com.sinema.auth.kullanici.repository.KullaniciRepository;
import com.sinema.auth.kullanici.service.KullaniciKayitServisi;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// Gerçek MySQL ile uçtan uca: controller → doğrulama → servis → JPA → Flyway şeması
@SpringBootTest
@AutoConfigureMockMvc
@Import(MysqlTestYapilandirmasi.class)
class KayitKontrolcusuEntegrasyonTest {

    private static final String KAYIT_ADRESI = "/api/v1/auth/register";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KullaniciRepository kullaniciRepository;

    @Test
    void shouldCreateUserWithHashedPassword_whenRequestIsValid() throws Exception {
        String email = benzersizEmail();

        kayitOl(email, "Parola123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());

        Kullanici kullanici = kullaniciRepository.findByEmail(email).orElseThrow();
        assertThat(kullanici.getParolaOzeti()).startsWith("$2a$12$").doesNotContain("Parola123");
        assertThat(kullanici.getOlusturulmaZamani()).isNotNull();
        assertThat(kullanici.getAd()).isEqualTo("Ali");
        assertThat(kullanici.getTelefon()).isEqualTo("+905321234567");
    }

    @Test
    void shouldIgnoreRoleField_whenClientTriesToRegisterAsAdmin() throws Exception {
        String email = benzersizEmail();
        String govde = ("{\"email\":\"%s\",\"password\":\"Parola123\",\"firstName\":\"Ali\",\"lastName\":\"Yılmaz\","
                + "\"phone\":\"05321234567\",\"role\":\"ADMIN\"}").formatted(email);

        mockMvc.perform(post(KAYIT_ADRESI).contentType(MediaType.APPLICATION_JSON).content(govde))
                .andExpect(status().isCreated());

        assertThat(kullaniciRepository.findByEmail(email).orElseThrow().getRol()).isEqualTo(Rol.USER);
    }

    @Test
    void shouldReturnConflict_whenEmailRegisteredWithDifferentCase() throws Exception {
        String email = benzersizEmail();
        kayitOl(email, "Parola123").andExpect(status().isCreated());

        kayitOl(email.toUpperCase(), "Parola456")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(KullaniciKayitServisi.EMAIL_KAYITLI_KODU));
    }

    @Test
    void shouldReturnFieldErrors_whenEmailAndPasswordInvalid() throws Exception {
        kayitOl("gecersiz-eposta", "kisa")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void shouldReturnFieldErrors_whenPersonalInfoMissingOrPhoneInvalid() throws Exception {
        kayitOl(benzersizEmail(), "Parola123", " ", "Yılmaz", "12345")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
                        "firstName", "firstName", "phone")));
        kayitOl(benzersizEmail(), "Parola123", "Ali", "Yılmaz", "0212 123 45 67")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("phone"));
        kayitOl(benzersizEmail(), "Parola123", "Ali<script>", "Yılmaz", "05321234567")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("firstName"));
    }

    private ResultActions kayitOl(String email, String parola) throws Exception {
        return kayitOl(email, parola, "Ali", "Yılmaz", "0532 123 45 67");
    }

    private ResultActions kayitOl(String email, String parola, String ad, String soyad, String telefon)
            throws Exception {
        String govde = "{\"email\":\"%s\",\"password\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"phone\":\"%s\"}"
                .formatted(email, parola, ad, soyad, telefon);
        return mockMvc.perform(post(KAYIT_ADRESI).contentType(MediaType.APPLICATION_JSON).content(govde));
    }

    // Testler aynı veritabanını paylaşır; her test kendi e-postasıyla çalışarak birbirini etkilemez
    private static String benzersizEmail() {
        return "kullanici-" + UUID.randomUUID() + "@ornek.com";
    }
}
