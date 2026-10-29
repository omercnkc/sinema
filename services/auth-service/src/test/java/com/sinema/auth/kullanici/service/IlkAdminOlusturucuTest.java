package com.sinema.auth.kullanici.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sinema.auth.config.KimlikAyarlari;
import com.sinema.auth.kullanici.entity.Rol;
import com.sinema.auth.kullanici.repository.KullaniciRepository;
import com.sinema.common.hata.CakismaIstisnasi;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class IlkAdminOlusturucuTest {

    private final KullaniciRepository kullaniciRepository = mock(KullaniciRepository.class);
    private final KullaniciKayitServisi kayitServisi = mock(KullaniciKayitServisi.class);
    private final Validator dogrulayici = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldDoNothing_whenAdminEmailNotConfigured() {
        olusturucu(new KimlikAyarlari.IlkAdmin("", "")).run(null);

        verify(kayitServisi, never()).adminEkle(any());
    }

    @Test
    void shouldNotCreateOrOverwrite_whenAnAdminAlreadyExists() {
        when(kullaniciRepository.existsByRol(Rol.ADMIN)).thenReturn(true);

        olusturucu(new KimlikAyarlari.IlkAdmin("admin@sinema.local", "AdminParola123")).run(null);

        verify(kayitServisi, never()).adminEkle(any());
    }

    @Test
    void shouldFailStartup_whenAdminPasswordViolatesPolicy() {
        assertThatThrownBy(() -> olusturucu(new KimlikAyarlari.IlkAdmin("admin@sinema.local", "kisa")).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
        verify(kayitServisi, never()).adminEkle(any());
    }

    @Test
    void shouldNotPromoteExistingUser_whenEmailBelongsToRegularAccount() {
        when(kayitServisi.adminEkle(any())).thenThrow(new CakismaIstisnasi("EMAIL_ALREADY_REGISTERED", "kayıtlı"));

        // İstisna dışarı sızmaz, servis açılmaya devam eder; yükseltme yapılmaz (adminEkle dışında yazma yok)
        olusturucu(new KimlikAyarlari.IlkAdmin("ali@ornek.com", "AdminParola123")).run(null);
    }

    private IlkAdminOlusturucu olusturucu(KimlikAyarlari.IlkAdmin ilkAdmin) {
        KimlikAyarlari ayarlar = new KimlikAyarlari(12, null, null, null, ilkAdmin, null);
        return new IlkAdminOlusturucu(ayarlar, kullaniciRepository, kayitServisi, dogrulayici);
    }
}
