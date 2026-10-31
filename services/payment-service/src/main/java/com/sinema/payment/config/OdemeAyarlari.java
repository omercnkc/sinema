package com.sinema.payment.config;

import com.sinema.payment.odeme.entity.OdemeSaglayicisi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * payment-service'in iş ayarları (sinema.payment.*); koda gömülmez (planlama §6.4).
 *
 * @param saglayici      ödemeyi alan sağlayıcı (K49): IYZICO (varsayılan) ya da MOCK (testler ve anahtarsız yerel deneme)
 * @param callbackAdresi sağlayıcının ödeme sonrası tarayıcıyı yönlendirdiği adres (gateway üzerinden, K39)
 * @param sonucAdresi    callback işlendikten sonra tarayıcının yönlendirildiği ön yüz sayfası (§8.6)
 * @param alici          sağlayıcının istediği ama toplamadığımız alıcı alanları (K48)
 */
@Validated
@ConfigurationProperties(prefix = "sinema.payment")
public record OdemeAyarlari(
        @NotNull OdemeSaglayicisi saglayici,
        @NotNull URI callbackAdresi,
        @NotNull URI sonucAdresi,
        @Valid @NotNull SabitAliciBilgileri alici) {

    /**
     * K48: TC kimlik no ve adres toplanmaz; iyzico zorunlu tuttuğu için sabit sandbox değerleri gönderilir (K39).
     * yedek*: kişi bilgisi olmayan hesaplar (admin, eski hesaplar) için.
     */
    public record SabitAliciBilgileri(
            @NotBlank String kimlikNo,
            @NotBlank String adres,
            @NotBlank String sehir,
            @NotBlank String ulke,
            @NotBlank String yedekAd,
            @NotBlank String yedekSoyad,
            @NotBlank String yedekTelefon) {
    }
}
