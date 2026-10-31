package com.sinema.payment.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Mutabakat job'ı ayarları (sinema.payment.mutabakat.*, planlama §4.8, K43).
 *
 * @param zamanlayiciEtkin  job'ın kendiliğinden çalışması (testlerde kapatılır, servis doğrudan çağrılır)
 * @param aralik            iki çalışma arası
 * @param partiBoyutu       bir çalışmada her işten en çok kaç ödeme
 * @param sakinlikSuresi    bu kadar süredir dokunulmamış kayıt alınır: callback'in hâlâ işlediği ödemeyle yarışılmaz
 * @param iadeBeklemesi     reddedilen ya da iletilemeyen iade bu kadar sonra yeniden denenir
 * @param kayitSaklama      ham sağlayıcı kayıtlarının saklanma süresi (K43: 90 gün)
 * @param temizlikZamani    eski kayıt temizliğinin cron ifadesi (UTC)
 */
@Validated
@ConfigurationProperties(prefix = "sinema.payment.mutabakat")
public record MutabakatAyarlari(
        @DefaultValue("true") boolean zamanlayiciEtkin,
        @NotNull @DefaultValue("1m") Duration aralik,
        @Min(1) @DefaultValue("100") int partiBoyutu,
        @NotNull @DefaultValue("2m") Duration sakinlikSuresi,
        @NotNull @DefaultValue("10m") Duration iadeBeklemesi,
        @NotNull @DefaultValue("90d") Duration kayitSaklama,
        @NotBlank @DefaultValue("0 30 3 * * *") String temizlikZamani) {
}
