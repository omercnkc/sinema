package com.sinema.payment.saglayici.iyzico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.payment.saglayici.BaslatmaIstegi;
import com.sinema.payment.saglayici.BaslatmaSonucu;
import com.sinema.payment.saglayici.SorguDurumu;
import com.sinema.payment.saglayici.SorguSonucu;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * iyzico adaptörü, iyzico'nun yerine geçen sahte HTTP sunucusuyla. İmzalar adaptörün koduyla değil, test içindeki
 * bağımsız bir HMAC hesabıyla doğrulanır (algoritma planlama §9.3-3'te gerçek sandbox'a karşı doğrulandı).
 */
class IyzicoOdemeSaglayicisiTest {

    private static final String TABAN = "https://sandbox-api.iyzipay.com";
    private static final String API_ANAHTARI = "sandbox-api";
    private static final String GIZLI = "sandbox-gizli";
    private static final String RASTGELE = "1793440000000123456789";
    private static final Instant SIMDI = Instant.parse("2026-11-01T10:00:00Z");

    private MockRestServiceServer sahteIyzico;
    private IyzicoOdemeSaglayicisi saglayici;

    @BeforeEach
    void kur() {
        RestClient.Builder kurucu = RestClient.builder().baseUrl(TABAN);
        sahteIyzico = MockRestServiceServer.bindTo(kurucu).build();
        saglayici = new IyzicoOdemeSaglayicisi(kurucu.build(),
                new IyzicoImzalayici(API_ANAHTARI, GIZLI, () -> RASTGELE), Clock.fixed(SIMDI, ZoneOffset.UTC));
    }

    @Test
    void shouldSignRequestOverExactBodyBytes_andSendSingleVirtualBasketItem() {
        sahteIyzico.expect(requestTo(TABAN + IyzicoOdemeSaglayicisi.BASLATMA_YOLU))
                .andExpect(method(HttpMethod.POST))
                .andExpect(istek -> {
                    MockClientHttpRequest mock = (MockClientHttpRequest) istek;
                    String govde = mock.getBodyAsString();
                    String beklenenImza = hmac(RASTGELE + IyzicoOdemeSaglayicisi.BASLATMA_YOLU + govde);
                    String yetki = new String(Base64.getDecoder().decode(
                            mock.getHeaders().getFirst("Authorization").substring("IYZWSv2 ".length())),
                            StandardCharsets.UTF_8);
                    assertThat(yetki).isEqualTo("apiKey:" + API_ANAHTARI + "&randomKey:" + RASTGELE
                            + "&signature:" + beklenenImza);
                    assertThat(mock.getHeaders().getFirst("x-iyzi-rnd")).isEqualTo(RASTGELE);
                })
                .andExpect(content().json("""
                        {"price":"300.00","paidPrice":"300.00","currency":"TRY","basketId":"9",
                         "conversationId":"konusma-1","callbackUrl":"http://localhost:8080/api/v1/payments/callback/iyzico",
                         "buyer":{"name":"Ayşe","gsmNumber":"+905551112233","identityNumber":"11111111111"},
                         "basketItems":[{"id":"9","itemType":"VIRTUAL","price":"300.00"}]}"""))
                .andRespond(withSuccess(baslatmaCevabi("token-1", hmac("konusma-1:token-1")), MediaType.APPLICATION_JSON));

        BaslatmaSonucu sonuc = saglayici.baslat(istek());

        assertThat(sonuc.token()).isEqualTo("token-1");
        assertThat(sonuc.tokenBitisi()).isEqualTo(SIMDI.plusSeconds(1800));
        assertThat(sonuc.odemeSayfasi()).hasToString("https://sandbox-cpp.iyzipay.com?token=token-1&lang=tr");
        sahteIyzico.verify();
    }

    @Test
    void shouldRejectStart_whenResponseSignatureInvalidOrStatusFailure() {
        sahteIyzico.expect(requestTo(TABAN + IyzicoOdemeSaglayicisi.BASLATMA_YOLU))
                .andRespond(withSuccess(baslatmaCevabi("token-1", "sahte-imza"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> saglayici.baslat(istek())).isInstanceOf(BagimlilikKullanilamazIstisnasi.class);

        sahteIyzico.reset();
        sahteIyzico.expect(requestTo(TABAN + IyzicoOdemeSaglayicisi.BASLATMA_YOLU)).andRespond(withSuccess("""
                {"status":"failure","errorCode":"5062","errorMessage":"Tutar uyuşmuyor"}""", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> saglayici.baslat(istek())).isInstanceOf(BagimlilikKullanilamazIstisnasi.class);
    }

    @Test
    void shouldReturnSuccessWithPaidAmount_whenPaymentApprovedAndSignatureValid() {
        String imza = hmac("SUCCESS:22416032:TRY:9:konusma-1:300:300:token-1"); // fiyatlarda sondaki sıfırlar atılır
        sahteIyzico.expect(requestTo(TABAN + IyzicoOdemeSaglayicisi.SORGU_YOLU))
                .andExpect(content().json("{\"token\":\"token-1\",\"conversationId\":\"konusma-1\"}"))
                .andRespond(withSuccess(sorguCevabi("SUCCESS", "1", imza), MediaType.APPLICATION_JSON));

        SorguSonucu sonuc = saglayici.sonucuSorgula("token-1", "konusma-1");

        assertThat(sonuc.durum()).isEqualTo(SorguDurumu.BASARILI);
        assertThat(sonuc.saglayiciOdemeKimligi()).isEqualTo("22416032");
        assertThat(sonuc.eslesiyor(new BigDecimal("300.00"), "TRY", "9", "konusma-1")).isTrue();
        assertThat(sonuc.maskeliGovde()).contains("\"paymentId\":\"22416032\"")
                .doesNotContain("552879").doesNotContain("0008").doesNotContain("DENEME KART");
    }

    @Test
    void shouldNotTrustResult_whenRetrieveSignatureInvalid() {
        sahteIyzico.expect(requestTo(TABAN + IyzicoOdemeSaglayicisi.SORGU_YOLU))
                .andRespond(withSuccess(sorguCevabi("SUCCESS", "1", "degistirilmis"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> saglayici.sonucuSorgula("token-1", "konusma-1"))
                .isInstanceOf(BagimlilikKullanilamazIstisnasi.class);
    }

    @Test
    void shouldMapFraudReviewAndDeclineAndNotPaidAndUnknownToken() {
        assertThat(sorgula(sorguCevabi("SUCCESS", "0", hmac("SUCCESS:22416032:TRY:9:konusma-1:300:300:token-1"))))
                .isEqualTo(SorguDurumu.BEKLIYOR);
        assertThat(sorgula(sorguCevabi("FAILURE", "1", hmac("FAILURE:22416032:TRY:9:konusma-1:300:300:token-1"))))
                .isEqualTo(SorguDurumu.BASARISIZ);
        assertThat(sorgula("{\"status\":\"failure\",\"errorCode\":\"5122\"}")).isEqualTo(SorguDurumu.BEKLIYOR);
        assertThat(sorgula("{\"status\":\"failure\",\"errorCode\":\"5124\"}")).isEqualTo(SorguDurumu.BULUNAMADI);
    }

    @Test
    void shouldRetryQueryOnce_whenConnectionFailsThenSucceeds() {
        sahteIyzico.expect(requestTo(TABAN + IyzicoOdemeSaglayicisi.SORGU_YOLU)).andRespond(withServerError());
        sahteIyzico.expect(requestTo(TABAN + IyzicoOdemeSaglayicisi.SORGU_YOLU))
                .andRespond(withSuccess("{\"status\":\"failure\",\"errorCode\":\"5122\"}", MediaType.APPLICATION_JSON));

        assertThat(saglayici.sonucuSorgula("t", "k").durum()).isEqualTo(SorguDurumu.BEKLIYOR);
        sahteIyzico.verify();
    }

    @Test
    void shouldNotRetryRefund_whenConnectionFails() {
        sahteIyzico.expect(ExpectedCount.once(), requestTo(TABAN + IyzicoOdemeSaglayicisi.IADE_YOLU))
                .andRespond(withServerError());

        assertThatThrownBy(() -> saglayici.iadeEt("22416032", BigDecimal.TEN, "k"))
                .isInstanceOf(BagimlilikKullanilamazIstisnasi.class);
        sahteIyzico.verify();
    }

    @Test
    void shouldReportUnavailable_whenServerErrorOrInvalidCredentials() {
        sahteIyzico.expect(ExpectedCount.times(2), requestTo(TABAN + IyzicoOdemeSaglayicisi.SORGU_YOLU))
                .andRespond(withServerError());
        assertThatThrownBy(() -> saglayici.sonucuSorgula("t", "k")).isInstanceOf(BagimlilikKullanilamazIstisnasi.class);

        sahteIyzico.reset();
        sahteIyzico.expect(requestTo(TABAN + IyzicoOdemeSaglayicisi.SORGU_YOLU)).andRespond(withSuccess(
                "{\"status\":\"failure\",\"errorCode\":\"1000\",\"errorMessage\":\"Geçersiz imza\"}",
                MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> saglayici.sonucuSorgula("t", "k")).isInstanceOf(BagimlilikKullanilamazIstisnasi.class);
    }

    @Test
    void shouldRefundWithV2ByPaymentId_andReportRejection() {
        sahteIyzico.expect(requestTo(TABAN + IyzicoOdemeSaglayicisi.IADE_YOLU))
                .andExpect(content().json("{\"paymentId\":\"22416032\",\"price\":\"300.00\",\"currency\":\"TRY\"}"))
                .andRespond(withSuccess("{\"status\":\"success\"}", MediaType.APPLICATION_JSON));
        assertThat(saglayici.iadeEt("22416032", new BigDecimal("300.00"), "konusma-1").basarili()).isTrue();

        sahteIyzico.reset();
        sahteIyzico.expect(requestTo(TABAN + IyzicoOdemeSaglayicisi.IADE_YOLU)).andRespond(withSuccess(
                "{\"status\":\"failure\",\"errorCode\":\"5093\"}", MediaType.APPLICATION_JSON));
        assertThat(saglayici.iadeEt("22416032", new BigDecimal("300.00"), "konusma-1").hataKodu()).isEqualTo("5093");
    }

    private SorguDurumu sorgula(String cevap) {
        sahteIyzico.reset();
        sahteIyzico.expect(requestTo(TABAN + IyzicoOdemeSaglayicisi.SORGU_YOLU))
                .andRespond(withSuccess(cevap, MediaType.APPLICATION_JSON));
        return saglayici.sonucuSorgula("token-1", "konusma-1").durum();
    }

    private static String baslatmaCevabi(String token, String imza) {
        return """
                {"status":"success","conversationId":"konusma-1","token":"%s","tokenExpireTime":1800,
                 "paymentPageUrl":"https://sandbox-cpp.iyzipay.com?token=%s&lang=tr","signature":"%s"}"""
                .formatted(token, token, imza);
    }

    /** Tutarlar iyzico'nun döndüğü gibi sayı (300.0); kart alanları maskelenmeli. */
    private static String sorguCevabi(String odemeDurumu, String fraud, String imza) {
        return """
                {"status":"success","paymentStatus":"%s","fraudStatus":%s,"paymentId":"22416032","price":300.0,
                 "paidPrice":300.0,"currency":"TRY","basketId":"9","conversationId":"konusma-1","token":"token-1",
                 "binNumber":"552879","lastFourDigits":"0008","cardHolderName":"DENEME KART","signature":"%s"}"""
                .formatted(odemeDurumu, fraud, imza);
    }

    private static BaslatmaIstegi istek() {
        return new BaslatmaIstegi("9", "konusma-1", new BigDecimal("300.00"), "TRY", "Sinema bileti, rezervasyon #9",
                new BaslatmaIstegi.Alici("42", "Ayşe", "Demir", "ayse@ornek.com", "+905551112233", "11111111111",
                        "Kadıköy", "Istanbul", "Turkey"),
                URI.create("http://localhost:8080/api/v1/payments/callback/iyzico"));
    }

    private static String hmac(String veri) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(GIZLI.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(veri.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
