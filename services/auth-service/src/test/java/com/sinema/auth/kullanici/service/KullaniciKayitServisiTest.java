package com.sinema.auth.kullanici.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sinema.auth.kullanici.dto.KayitCevabi;
import com.sinema.auth.kullanici.dto.KayitIstegi;
import com.sinema.auth.kullanici.entity.Kullanici;
import com.sinema.auth.kullanici.entity.Rol;
import com.sinema.auth.kullanici.repository.KullaniciRepository;
import com.sinema.common.hata.CakismaIstisnasi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class KullaniciKayitServisiTest {

    @Mock
    private KullaniciRepository kullaniciRepository;

    @Mock
    private PasswordEncoder parolaKodlayici;

    @InjectMocks
    private KullaniciKayitServisi servis;

    @Test
    void shouldSaveNormalizedEmailHashedPasswordAndUserRole_whenEmailIsNew() {
        // Given
        when(kullaniciRepository.existsByEmail("ali@ornek.com")).thenReturn(false);
        when(parolaKodlayici.encode("Parola123")).thenReturn("$2a$ozet");
        when(kullaniciRepository.saveAndFlush(any())).thenAnswer(cagri -> cagri.getArgument(0));

        // When
        KayitCevabi cevap = servis.kaydet(new KayitIstegi("  ALI@Ornek.COM ", "Parola123"));

        // Then
        ArgumentCaptor<Kullanici> kaydedilen = ArgumentCaptor.forClass(Kullanici.class);
        verify(kullaniciRepository).saveAndFlush(kaydedilen.capture());
        assertThat(kaydedilen.getValue().getEmail()).isEqualTo("ali@ornek.com");
        assertThat(kaydedilen.getValue().getParolaOzeti()).isEqualTo("$2a$ozet");
        assertThat(kaydedilen.getValue().getRol()).isEqualTo(Rol.USER);
        assertThat(cevap.email()).isEqualTo("ali@ornek.com");
    }

    @Test
    void shouldThrowConflict_whenEmailAlreadyRegistered() {
        when(kullaniciRepository.existsByEmail("ali@ornek.com")).thenReturn(true);

        assertThatThrownBy(() -> servis.kaydet(new KayitIstegi("ali@ornek.com", "Parola123")))
                .isInstanceOf(CakismaIstisnasi.class)
                .extracting("kod").isEqualTo(KullaniciKayitServisi.EMAIL_KAYITLI_KODU);
        verify(kullaniciRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowConflict_whenConcurrentRegistrationHitsUniqueConstraint() {
        when(kullaniciRepository.existsByEmail("ali@ornek.com")).thenReturn(false);
        when(parolaKodlayici.encode(any())).thenReturn("$2a$ozet");
        when(kullaniciRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_users_email"));

        assertThatThrownBy(() -> servis.kaydet(new KayitIstegi("ali@ornek.com", "Parola123")))
                .isInstanceOf(CakismaIstisnasi.class);
    }

    @Test
    void shouldNormalizeWithoutTurkishDotlessI_whenEmailHasUppercaseI() {
        assertThat(KullaniciKayitServisi.emailNormalizeEt("INFO@SINEMA.COM")).isEqualTo("info@sinema.com");
    }
}
