package com.sinema.common.servisarasi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Çağrılan servisin hata cevabını (ortak problem+json biçimi) bu servisin istisnasına çevirir.
 *
 * 404 bir iş sonucudur ("seans yok"): çağrılan servisin kodu ve kullanıcıya gösterilebilir mesajı aynen taşınır,
 * devre kesici bunu hata saymaz. Diğer her durum (5xx, beklenmeyen 4xx = bizim hatamız) Feign'in varsayılan
 * istisnasına bırakılır; istemcinin fallback'i onu 503'e çevirir (BagimlilikHatalari).
 */
public class SinemaHataCozucu implements ErrorDecoder {

    private static final String VARSAYILAN_BULUNAMADI_KODU = "NOT_FOUND";
    private static final String VARSAYILAN_BULUNAMADI_MESAJI = "Aranan kayıt bulunamadı.";
    // Hata gövdesi küçük olmalı; beklenmedik büyük gövde belleği doldurmasın
    private static final int EN_FAZLA_GOVDE_BAYTI = 16 * 1024;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ErrorDecoder varsayilan = new ErrorDecoder.Default();

    @Override
    public Exception decode(String metotAnahtari, Response cevap) {
        if (cevap.status() == HttpStatus.NOT_FOUND.value()) {
            ProblemOzeti problem = problemOku(cevap);
            return new KaynakBulunamadiIstisnasi(
                    problem.code() != null ? problem.code() : VARSAYILAN_BULUNAMADI_KODU,
                    problem.detail() != null ? problem.detail() : VARSAYILAN_BULUNAMADI_MESAJI);
        }
        return varsayilan.decode(metotAnahtari, cevap);
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
