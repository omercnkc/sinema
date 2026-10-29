package com.sinema.auth.kullanici.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sinema.auth.kullanici.dto.KayitIstegi;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// İlk admin, .env'deki değişkenlerin karşılığı olan özelliklerle açılışta oluşturulur
@SpringBootTest(properties = {
        "sinema.auth.ilk-admin.email=" + AdminKontrolcusuEntegrasyonTest.ILK_ADMIN_EMAIL,
        "sinema.auth.ilk-admin.parola=" + AdminKontrolcusuEntegrasyonTest.ILK_ADMIN_PAROLA})
@AutoConfigureMockMvc
@Import(MysqlTestYapilandirmasi.class)
class AdminKontrolcusuEntegrasyonTest {

    static final String ILK_ADMIN_EMAIL = "ilk-admin@sinema.local";
    static final String ILK_ADMIN_PAROLA = "AdminParola123";
    private static final String ADMIN_EKLEME_ADRESI = "/api/v1/admin/auth/admins";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KullaniciRepository kullaniciRepository;

    @Autowired
    private KullaniciKayitServisi kayitServisi;

    @Test
    void shouldCreateFirstAdminOnStartup_whenAdminCredentialsConfigured() {
        assertThat(kullaniciRepository.findByEmail(ILK_ADMIN_EMAIL).orElseThrow().getRol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    void shouldCreateAnotherAdmin_whenCalledByAdmin() throws Exception {
        String yeniAdmin = "admin-" + UUID.randomUUID() + "@sinema.local";

        mockMvc.perform(adminEkle(yeniAdmin).header("Authorization", "Bearer " + token(ILK_ADMIN_EMAIL, ILK_ADMIN_PAROLA)))
                .andExpect(status().isCreated());

        assertThat(kullaniciRepository.findByEmail(yeniAdmin).orElseThrow().getRol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    void shouldReturnForbiddenProblemJson_whenCalledByRegularUser() throws Exception {
        String email = "kullanici-" + UUID.randomUUID() + "@ornek.com";
        kayitServisi.kaydet(new KayitIstegi(email, "Parola123"));

        mockMvc.perform(adminEkle("x-" + email).header("Authorization", "Bearer " + token(email, "Parola123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldReturnUnauthorized_whenCalledWithoutToken() throws Exception {
        mockMvc.perform(adminEkle("x@sinema.local")).andExpect(status().isUnauthorized());
    }

    private static MockHttpServletRequestBuilder adminEkle(String email) {
        return post(ADMIN_EKLEME_ADRESI).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"Parola123\"}".formatted(email));
    }

    private String token(String email, String parola) throws Exception {
        String govde = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, parola)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(govde, "$.accessToken");
    }
}
