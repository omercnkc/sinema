package com.sinema.common.servisarasi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Başka servisi Feign ile çağıran servisin "sinema.servis-istemcisi" ayarları (planlama §5.5, rules §5.7).
 * auth-adresi verilmezse Feign altyapısı hiç kurulmaz.
 *
 * @param authAdresi         servis token'ının alındığı auth-service adresi (Docker iç ağı)
 * @param sir                bu servisin client secret'ı; istemci kimliği spring.application.name'dir
 * @param tokenYenilemePayi  token süresi dolmadan bu kadar önce yenilenir (yolda süresi dolmasın)
 * @param baglantiZamanAsimi hem auth'a hem çağrılan servise bağlantı zaman aşımı
 * @param okumaZamanAsimi    cevap bekleme sınırı; aşılırsa çağrı başarısız sayılır (fail fast)
 */
@Validated
@ConfigurationProperties(prefix = "sinema.servis-istemcisi")
public record ServisIstemcisiAyarlari(
        @NotNull URI authAdresi,
        @NotBlank String sir,
        @NotNull @DefaultValue("30s") Duration tokenYenilemePayi,
        @NotNull @DefaultValue("2s") Duration baglantiZamanAsimi,
        @NotNull @DefaultValue("3s") Duration okumaZamanAsimi,
        @Valid @NotNull @DefaultValue YenidenDeneme yenidenDeneme,
        @Valid @NotNull @DefaultValue DevreKesici devreKesici) {

    /**
     * Yalnızca GET ve yalnızca ağ hatasında (bağlantı/zaman aşımı) uygulanır; bkz. GuvenliYontemYenidenDeneyici.
     *
     * @param enFazlaDeneme ilk deneme dahil toplam deneme sayısı
     */
    public record YenidenDeneme(
            @Min(1) @DefaultValue("3") int enFazlaDeneme,
            @NotNull @DefaultValue("100ms") Duration ilkBekleme,
            @NotNull @DefaultValue("1s") Duration enUzunBekleme) {
    }

    /**
     * Çağrılan servis sürekli hata veriyorsa bir süre hiç denenmez, hemen 503 dönülür (yığılma önlenir).
     *
     * @param pencereBoyutu    hata oranının hesaplandığı son çağrı sayısı
     * @param enAzCagri        oran hesaplanmadan önce gereken en az çağrı
     * @param hataOraniEsigi   yüzde; aşılınca devre açılır
     * @param acikKalmaSuresi  devre açıkken çağrı yapılmayan süre, sonra deneme çağrılarına izin verilir
     */
    public record DevreKesici(
            @Min(1) @DefaultValue("10") int pencereBoyutu,
            @Min(1) @DefaultValue("5") int enAzCagri,
            @Min(1) @DefaultValue("50") int hataOraniEsigi,
            @NotNull @DefaultValue("10s") Duration acikKalmaSuresi) {
    }
}
