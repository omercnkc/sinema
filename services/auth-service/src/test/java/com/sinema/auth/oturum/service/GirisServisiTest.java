package com.sinema.auth.oturum.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sinema.auth.config.KimlikAyarlari;
import com.sinema.auth.kullanici.entity.Kullanici;
import com.sinema.auth.kullanici.entity.Rol;
import com.sinema.auth.kullanici.repository.KullaniciRepository;
import com.sinema.auth.oturum.dto.GirisIstegi;
import com.sinema.common.hata.CokFazlaIstekIstisnasi;
import com.sinema.common.hata.YetkisizIstisnasi;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class GirisServisiTest {

    private static final Instant SIMDI = Instant.parse("2026-11-01T10:00:00Z");
    private static final String SAHTE_OZET = "$2a$sahte";

    private final KullaniciRepository kullaniciRepository = mock(KullaniciRepository.class);
    private final PasswordEncoder parolaKodlayici = mock(PasswordEncoder.class);
    private final ErisimTokeniUretici tokenUretici = mock(ErisimTokeniUretici.class);
    private GirisServisi servis;

    @BeforeEach
    void kur() {
        when(parolaKodlayici.encode(anyString())).thenReturn(SAHTE_OZET);
        KimlikAyarlari ayarlar = new KimlikAyarlari(12, null, new KimlikAyarlari.Kilit(5, Duration.ofMinutes(5)));
        servis = new GirisServisi(kullaniciRepository, parolaKodlayici, tokenUretici, ayarlar,
                Clock.fixed(SIMDI, ZoneOffset.UTC));
    }

    @Test
    void shouldStillRunPasswordCheck_whenEmailIsNotRegistered() {
        when(kullaniciRepository.findByEmail("yok@ornek.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servis.girisYap(new GirisIstegi("yok@ornek.com", "Parola123")))
                .isInstanceOf(YetkisizIstisnasi.class);
        verify(parolaKodlayici).matches("Parola123", SAHTE_OZET);
    }

    @Test
    void shouldRecordFailureWithLockDeadline_whenPasswordIsWrong() {
        Kullanici kullanici = kullanici(null);
        when(kullaniciRepository.findByEmail("ali@ornek.com")).thenReturn(Optional.of(kullanici));
        when(parolaKodlayici.matches("Yanlis123", "$2a$gercek")).thenReturn(false);

        assertThatThrownBy(() -> servis.girisYap(new GirisIstegi("ali@ornek.com", "Yanlis123")))
                .isInstanceOf(YetkisizIstisnasi.class);
        verify(kullaniciRepository).basarisizGirisiKaydet(7L, 5, SIMDI.plus(Duration.ofMinutes(5)));
    }

    @Test
    void shouldRejectWithoutCheckingPassword_whenAccountIsLocked() {
        Kullanici kullanici = kullanici(SIMDI.plusSeconds(90));
        when(kullaniciRepository.findByEmail("ali@ornek.com")).thenReturn(Optional.of(kullanici));

        assertThatThrownBy(() -> servis.girisYap(new GirisIstegi("ali@ornek.com", "Parola123")))
                .isInstanceOf(CokFazlaIstekIstisnasi.class)
                .extracting("kod").isEqualTo(GirisServisi.HESAP_KILITLI_KODU);
        verify(parolaKodlayici, never()).matches(any(), eq("$2a$gercek"));
        verify(kullaniciRepository, never()).basarisizGirisiKaydet(anyLong(), anyInt(), any());
    }

    private static Kullanici kullanici(Instant kilitBitisi) {
        Kullanici kullanici = Kullanici.olustur("ali@ornek.com", "$2a$gercek", Rol.USER);
        ReflectionTestUtils.setField(kullanici, "id", 7L);
        ReflectionTestUtils.setField(kullanici, "kilitBitisi", kilitBitisi);
        return kullanici;
    }
}
