package com.sinema.payment.saglayici.mock;

import static org.assertj.core.api.Assertions.assertThat;

import com.sinema.payment.saglayici.BaslatmaIstegi;
import com.sinema.payment.saglayici.BaslatmaSonucu;
import com.sinema.payment.saglayici.SorguDurumu;
import com.sinema.payment.saglayici.SorguSonucu;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class MockOdemeSaglayicisiTest {

    private static final Instant SIMDI = Instant.parse("2026-11-01T10:00:00Z");

    private final MockOdemeSaglayicisi saglayici = new MockOdemeSaglayicisi(
            new MockAyarlari(URI.create("http://localhost:8080/api/v1/payments/mock/checkout"), Duration.ofMinutes(30)),
            Clock.fixed(SIMDI, ZoneOffset.UTC));

    @Test
    void shouldReturnPageUnderMockAddressWithThirtyMinuteToken_whenStarted() {
        BaslatmaSonucu sonuc = saglayici.baslat(istek());

        assertThat(sonuc.odemeSayfasi().toString())
                .isEqualTo("http://localhost:8080/api/v1/payments/mock/checkout/" + sonuc.token());
        assertThat(sonuc.tokenBitisi()).isEqualTo(SIMDI.plus(Duration.ofMinutes(30)));
    }

    @Test
    void shouldBePendingUntilUserChooses_thenSucceedWithMatchingFields() {
        String token = saglayici.baslat(istek()).token();
        assertThat(saglayici.sonucuSorgula(token, "konusma-1").durum()).isEqualTo(SorguDurumu.BEKLIYOR);

        assertThat(saglayici.sonucuKaydet(token, true)).isTrue();
        SorguSonucu sonuc = saglayici.sonucuSorgula(token, "konusma-1");

        assertThat(sonuc.durum()).isEqualTo(SorguDurumu.BASARILI);
        assertThat(sonuc.saglayiciOdemeKimligi()).startsWith("mock-");
        assertThat(sonuc.eslesiyor(new BigDecimal("300.0"), "TRY", "9", "konusma-1")).isTrue();
        assertThat(sonuc.eslesiyor(new BigDecimal("299.99"), "TRY", "9", "konusma-1")).isFalse();
    }

    @Test
    void shouldKeepFirstChoice_whenUserSubmitsAgainWithDifferentResult() {
        String token = saglayici.baslat(istek()).token();
        saglayici.sonucuKaydet(token, false);

        assertThat(saglayici.sonucuKaydet(token, true)).isFalse();
        assertThat(saglayici.sonucuSorgula(token, "konusma-1").durum()).isEqualTo(SorguDurumu.BASARISIZ);
    }

    @Test
    void shouldReportNotFound_whenTokenUnknown() {
        assertThat(saglayici.sonucuSorgula("olmayan", "x").durum()).isEqualTo(SorguDurumu.BULUNAMADI);
        assertThat(saglayici.sonucuKaydet("olmayan", true)).isFalse();
    }

    @Test
    void shouldRefundPaidPayment_andRejectUnknownPayment() {
        String token = saglayici.baslat(istek()).token();
        saglayici.sonucuKaydet(token, true);
        String odemeKimligi = saglayici.sonucuSorgula(token, "konusma-1").saglayiciOdemeKimligi();

        assertThat(saglayici.iadeEt(odemeKimligi, new BigDecimal("300.00"), "konusma-1").basarili()).isTrue();
        assertThat(saglayici.iadeEdildiMi(token)).isTrue();
        assertThat(saglayici.iadeEt("mock-yok", BigDecimal.ONE, "x").basarili()).isFalse();
    }

    private static BaslatmaIstegi istek() {
        return new BaslatmaIstegi("9", "konusma-1", new BigDecimal("300.00"), "TRY", "Sinema bileti",
                new BaslatmaIstegi.Alici("42", "Ayşe", "Demir", "a@b.com", "+905551112233", "11111111111",
                        "Kadıköy", "Istanbul", "Turkey"),
                URI.create("http://localhost:8080/api/v1/payments/callback/iyzico"));
    }
}
