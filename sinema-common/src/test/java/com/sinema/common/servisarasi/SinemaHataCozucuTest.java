package com.sinema.common.servisarasi;

import static org.assertj.core.api.Assertions.assertThat;

import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import com.sinema.common.hata.SinemaIstisnasi;
import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SinemaHataCozucuTest {

    private final SinemaHataCozucu cozucu = new SinemaHataCozucu();

    @Test
    void shouldCarryCalleeCodeAndMessage_whenNotFoundWithProblemJson() {
        Exception hata = cozucu.decode("KatalogIstemcisi#seansBilgisi(Long)", cevap(404, """
                {"type":null,"title":"Not Found","status":404,"code":"SHOWTIME_NOT_FOUND","detail":"Seans bulunamadı."}
                """));

        assertThat(hata).isInstanceOf(KaynakBulunamadiIstisnasi.class).hasMessage("Seans bulunamadı.");
        assertThat(((SinemaIstisnasi) hata).getKod()).isEqualTo("SHOWTIME_NOT_FOUND");
    }

    @Test
    void shouldUseGenericNotFound_whenBodyIsNotProblemJson() {
        Exception hata = cozucu.decode("x", cevap(404, "<html>Not Found</html>"));

        assertThat(hata).isInstanceOf(KaynakBulunamadiIstisnasi.class);
        assertThat(((SinemaIstisnasi) hata).getKod()).isEqualTo("NOT_FOUND");
    }

    @Test
    void shouldLeaveServerErrorsToFeign_whenStatusIs5xx() {
        Exception hata = cozucu.decode("x", cevap(500, "{\"code\":\"INTERNAL_ERROR\"}"));

        assertThat(hata).isInstanceOf(FeignException.class);
    }

    @Test
    void shouldTreatOnlyNonAvailabilityBusinessErrorsAsCallOutcome() {
        assertThat(BagimlilikHatalari.isSonucuMu(new KaynakBulunamadiIstisnasi("X", "x"))).isTrue();
        assertThat(BagimlilikHatalari.isSonucuMu(new BagimlilikKullanilamazIstisnasi("X", "x"))).isFalse();
        assertThat(BagimlilikHatalari.isSonucuMu(new IllegalStateException())).isFalse();
    }

    @Test
    void shouldConvertUnexpectedErrorToServiceUnavailable_butKeepBusinessError() {
        KaynakBulunamadiIstisnasi isHatasi = new KaynakBulunamadiIstisnasi("SHOWTIME_NOT_FOUND", "Seans bulunamadı.");

        assertThat(BagimlilikHatalari.cevir(isHatasi, "CATALOG_UNAVAILABLE", "m")).isSameAs(isHatasi);
        assertThat(BagimlilikHatalari.cevir(new IllegalStateException(), "CATALOG_UNAVAILABLE", "m"))
                .isInstanceOf(BagimlilikKullanilamazIstisnasi.class)
                .extracting(SinemaIstisnasi::getKod).isEqualTo("CATALOG_UNAVAILABLE");
    }

    private static Response cevap(int durum, String govde) {
        Request istek = Request.create(Request.HttpMethod.GET, "http://catalog-service/x", Map.of(), null,
                StandardCharsets.UTF_8, null);
        return Response.builder()
                .status(durum)
                .request(istek)
                .headers(Map.of())
                .body(govde, StandardCharsets.UTF_8)
                .build();
    }
}
