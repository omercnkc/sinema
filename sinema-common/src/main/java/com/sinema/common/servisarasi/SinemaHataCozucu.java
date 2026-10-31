package com.sinema.common.servisarasi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.IsKuraliIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Çağrılan servisin hata cevabını (ortak problem+json biçimi) bu servisin istisnasına çevirir.
 *
 * 404 (kayıt yok), 409 (çakışma, ör. "rezervasyonun süresi doldu") ve 422 (iş kuralı) birer iş sonucudur: çağrılan
 * servisin kodu ve kullanıcıya gösterilebilir mesajı aynen taşınır, devre kesici bunları hata saymaz. Çağıran bu
 * sonuçlara göre karar verir (ör. payment, onay 409 alırsa iade başlatır). Diğer her durum (5xx, beklenmeyen 4xx =
 * bizim hatamız) Feign'in varsayılan istisnasına bırakılır; istemcinin fallback'i onu 503'e çevirir.
 */
public class SinemaHataCozucu implements ErrorDecoder {

    private static final Set<HttpStatus> IS_SONUCU_DURUMLARI =
            Set.of(HttpStatus.NOT_FOUND, HttpStatus.CONFLICT, HttpStatus.UNPROCESSABLE_CONTENT);
    // Gövde problem+json değilse (ör. ara katmandan gelen HTML) kullanılan genel mesajlar
    private static final Map<HttpStatus, String> VARSAYILAN_MESAJLAR = Map.of(
            HttpStatus.NOT_FOUND, "Aranan kayıt bulunamadı.",
            HttpStatus.CONFLICT, "İşlem şu anki durumla çakışıyor.",
            HttpStatus.UNPROCESSABLE_CONTENT, "İşlem kurallara uymuyor.");
    // Hata gövdesi küçük olmalı; beklenmedik büyük gövde belleği doldurmasın
    private static final int EN_FAZLA_GOVDE_BAYTI = 16 * 1024;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ErrorDecoder varsayilan = new ErrorDecoder.Default();

    @Override
    public Exception decode(String metotAnahtari, Response cevap) {
        HttpStatus durum = HttpStatus.resolve(cevap.status());
        if (durum == null || !IS_SONUCU_DURUMLARI.contains(durum)) {
            return varsayilan.decode(metotAnahtari, cevap);
        }
        ProblemOzeti problem = problemOku(cevap);
        String kod = problem.code() != null ? problem.code() : durum.name();
        String mesaj = problem.detail() != null ? problem.detail() : VARSAYILAN_MESAJLAR.get(durum);
        return switch (durum) {
            case NOT_FOUND -> new KaynakBulunamadiIstisnasi(kod, mesaj);
            case CONFLICT -> new CakismaIstisnasi(kod, mesaj);
            default -> new IsKuraliIstisnasi(kod, mesaj);
        };
    }

    private ProblemOzeti problemOku(Response cevap) {
        if (cevap.body() == null) {
            return ProblemOzeti.BOS;
        }
        try (InputStream girdi = cevap.body().asInputStream()) {
            byte[] govde = girdi.readNBytes(EN_FAZLA_GOVDE_BAYTI);
            return govde.length == 0 ? ProblemOzeti.BOS : jsonMapper.readValue(govde, ProblemOzeti.class);
        } catch (IOException | JacksonException e) {
            // Gövde problem+json değilse (ör. ara katmandan gelen HTML) genel mesajla devam edilir
            return ProblemOzeti.BOS;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProblemOzeti(String code, String detail) {

        static final ProblemOzeti BOS = new ProblemOzeti(null, null);
    }
}
