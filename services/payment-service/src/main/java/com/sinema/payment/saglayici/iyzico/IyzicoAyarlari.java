package com.sinema.payment.saglayici.iyzico;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * iyzico ayarları (sinema.payment.iyzico.*), yalnızca saglayici=IYZICO iken okunur. Anahtarların varsayılanı yok:
 * eksikse servis açılmaz (fail fast). Yalnızca sandbox kullanılır (K39).
 *
 * @param baglantiZamanAsimi Docker Desktop NAT'ında yeni hedefe ilk bağlantı ~3 sn sürebiliyor (ölçüldü)
 * @param okumaZamanAsimi iyzico ödeme sonucu sorgusu birkaç saniye sürebilir; kullanıcı bekletilmeden 503'e düşülür
 */
@Validated
@ConfigurationProperties(prefix = "sinema.payment.iyzico")
public record IyzicoAyarlari(
        @NotBlank String apiAnahtari,
        @NotBlank String gizliAnahtar,
        @NotNull URI tabanAdres,
        @NotNull @DefaultValue("5s") Duration baglantiZamanAsimi,
        @NotNull @DefaultValue("10s") Duration okumaZamanAsimi) {

    @Override
    public String toString() {
        return "IyzicoAyarlari[apiAnahtari=***, gizliAnahtar=***, tabanAdres=" + tabanAdres + "]";
    }
}
