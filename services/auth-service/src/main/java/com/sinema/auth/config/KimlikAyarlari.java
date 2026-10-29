package com.sinema.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * auth-service'in application.yml'deki "sinema.auth" ayarları; değerler koda gömülmez.
 * Geçersiz değerle servis açılmaz (@Validated).
 *
 * @param bcryptMaliyeti BCrypt iş faktörü; her +1 hesaplama süresini ikiye katlar
 * @param ilkAdmin       .env'den gelen ilk admin; boşsa oluşturulmaz
 */
@Validated
@ConfigurationProperties(prefix = "sinema.auth")
public record KimlikAyarlari(
        @Min(10) @Max(16) int bcryptMaliyeti,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Kilit kilit,
        @Valid @NotNull Yenileme yenileme,
        IlkAdmin ilkAdmin,
        Map<String, ServisIstemcisi> servisIstemcileri) {

    public KimlikAyarlari {
        servisIstemcileri = servisIstemcileri == null ? Map.of() : Map.copyOf(servisIstemcileri);
    }

    /**
     * Yayıncı (iss) ve public key ortak "sinema.guvenlik.jwt" ayarındadır (tüm servisler doğrularken kullanır);
     * burada yalnızca imzalamaya özgü olanlar var.
     *
     * @param erisimTokenSuresi kullanıcı access token ömrü (K36: 15 dk)
     * @param servisTokenSuresi servisler arası token ömrü (planlama §5.5: 5 dk)
     * @param ozelAnahtar       PKCS#8 private key (yalnızca auth-service'te)
     */
    public record Jwt(
            @NotNull Duration erisimTokenSuresi,
            @NotNull Duration servisTokenSuresi,
            @NotNull Resource ozelAnahtar) {
    }

    /**
     * Servis token'ı alabilen bir servis (client credentials, planlama §5.5). Anahtar = clientId (servis adı).
     *
     * @param sir             paylaşılan sır (.env); boşsa bu istemci devre dışıdır
     * @param izinliHedefler  token isteyebileceği hedef servisler ("aud"); ör. payment yalnızca booking
     */
    public record ServisIstemcisi(String sir, List<String> izinliHedefler) {

        public ServisIstemcisi {
            izinliHedefler = izinliHedefler == null ? List.of() : List.copyOf(izinliHedefler);
        }

        @Override
        public String toString() {
            return "ServisIstemcisi[sir=***, izinliHedefler=" + izinliHedefler + "]";
        }
    }

    /**
     * @param esik kaç hatalı denemede kilitlenir (K36: 5)
     * @param sure kilit ne kadar sürer (K36: 5 dk)
     */
    public record Kilit(@Min(1) int esik, @NotNull Duration sure) {
    }

    /**
     * @param sure           refresh token ömrü (K36: 14 gün); her yenilemede yeniden başlar
     * @param tolerans       iki sekmenin aynı anda yenilemesine izin verilen süre (planlama §5.2: 10 sn)
     * @param cerezAdi       cookie adı
     * @param cerezYolu      cookie yalnızca bu yoldaki isteklere gider
     * @param guvenliCerez   Secure bayrağı
     * @param temizlikZamani süresi dolmuş token'ları silen job'ın cron ifadesi
     */
    public record Yenileme(
            @NotNull Duration sure,
            @NotNull Duration tolerans,
            @NotBlank String cerezAdi,
            @NotBlank String cerezYolu,
            boolean guvenliCerez,
            @NotBlank String temizlikZamani) {
    }

    public record IlkAdmin(String email, String parola) {

        @Override
        public String toString() {
            return "IlkAdmin[email=***, parola=***]";
        }
    }
}
