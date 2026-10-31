package com.sinema.payment.saglayici.iyzico;

import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.payment.odeme.entity.OdemeSaglayicisi;
import com.sinema.payment.saglayici.BaslatmaIstegi;
import com.sinema.payment.saglayici.BaslatmaSonucu;
import com.sinema.payment.saglayici.IadeSonucu;
import com.sinema.payment.saglayici.OdemeSaglayiciAdaptoru;
import com.sinema.payment.saglayici.SorguDurumu;
import com.sinema.payment.saglayici.SorguSonucu;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * iyzico Checkout Form adaptörü (K12, K49): kendi istemcimiz, resmi SDK yok. Kurallar planlama §9.3-3'te
 * (sandbox'ta doğrulandı):
 * - İş hataları HTTP 200 ile gelir; karar gövdedeki status'a göre verilir.
 * - İmzalanan gövde gönderilenle birebir aynı olmalı: JSON bir kez üretilir, aynı baytlar imzalanır ve gönderilir.
 * - Cevap imzası doğrulanmadan sonuca güvenilmez (başlatma: conversationId:token; sorgu: 8 alan).
 * - Ödenmemiş token 5122 (BEKLIYOR), tanınmayan token 5124 (BULUNAMADI).
 * Sağlayıcıya ulaşılamaz ya da beklenmeyen bir cevap gelirse 503 PAYMENT_PROVIDER_UNAVAILABLE.
 */
public class IyzicoOdemeSaglayicisi implements OdemeSaglayiciAdaptoru {

    private static final Logger log = LoggerFactory.getLogger(IyzicoOdemeSaglayicisi.class);

    static final String BASLATMA_YOLU = "/payment/iyzipos/checkoutform/initialize/auth/ecom";
    static final String SORGU_YOLU = "/payment/iyzipos/checkoutform/auth/ecom/detail";
    static final String IADE_YOLU = "/v2/payment/refund";
    static final String ODEME_YOK_KODU = "5122";
    static final String TOKEN_YOK_KODU = "5124";
    private static final String BASARILI = "success";
    private static final String ODEME_BASARILI = "SUCCESS";
    private static final String ODEME_BASARISIZ = "FAILURE";
    private static final String FRAUD_ONAYLI = "1";
    private static final String FRAUD_INCELEMEDE = "0";
    private static final String DIL = "tr";
    private static final int TEKRARLANABILIR = 2;
    private static final String KATEGORI = "Sinema";
    private static final String SANAL_URUN = "VIRTUAL";
    // Denetim kaydına yalnızca bu alanlar yazılır: kart bilgisi (bin, son dört hane, kart sahibi...) hiç kaydedilmez
    private static final Set<String> KAYDEDILECEK_ALANLAR = Set.of("status", "errorCode", "errorMessage",
            "paymentStatus", "fraudStatus", "paymentId", "price", "paidPrice", "currency", "basketId",
            "conversationId", "token", "installment", "phase", "mdStatus", "systemTime");

    private static final TypeReference<Map<String, Object>> CEVAP_TIPI = new TypeReference<>() { };

    private final RestClient istemci;
    private final IyzicoImzalayici imzalayici;
    private final Clock saat;
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS) // tutarlar double'a çevrilip yuvarlanmasın
            .build();

    public IyzicoOdemeSaglayicisi(RestClient istemci, IyzicoImzalayici imzalayici, Clock saat) {
        this.istemci = istemci;
        this.imzalayici = imzalayici;
        this.saat = saat;
    }

    @Override
    public OdemeSaglayicisi tur() {
        return OdemeSaglayicisi.IYZICO;
    }

    @Override
    public BaslatmaSonucu baslat(BaslatmaIstegi istek) {
        Map<String, Object> cevap = gonder(BASLATMA_YOLU, baslatmaGovdesi(istek), TEKRARLANABILIR);
        if (!BASARILI.equals(metin(cevap, "status"))) {
            throw saglayiciHatasi("başlatma", cevap);
        }
        String token = metin(cevap, "token");
        if (!imzalayici.cevapImzasiDogruMu(List.of(istek.konusmaKimligi(), token), metin(cevap, "signature"))) {
            throw imzaHatasi("başlatma");
        }
        long omurSaniye = Long.parseLong(metin(cevap, "tokenExpireTime"));
        return new BaslatmaSonucu(token, saat.instant().plusSeconds(omurSaniye),
                URI.create(metin(cevap, "paymentPageUrl")));
    }

    @Override
    public SorguSonucu sonucuSorgula(String token, String konusmaKimligi) {
        Map<String, Object> cevap = gonder(SORGU_YOLU, sirali("locale", DIL, "conversationId", konusmaKimligi,
                "token", token), TEKRARLANABILIR);
        String maskeli = maskele(cevap);
        if (!BASARILI.equals(metin(cevap, "status"))) {
            return basarisizSorgu(cevap, maskeli);
        }
        if (!imzalayici.cevapImzasiDogruMu(sorguImzaAlanlari(cevap), metin(cevap, "signature"))) {
            throw imzaHatasi("sorgu");
        }
        return new SorguSonucu(odemeDurumu(cevap), metin(cevap, "paymentId"), fiyat(cevap, "paidPrice"),
                metin(cevap, "currency"), metin(cevap, "basketId"), metin(cevap, "conversationId"),
                hataKoduSorgu(cevap), maskeli);
    }

    @Override
    public IadeSonucu iadeEt(String saglayiciOdemeKimligi, BigDecimal tutar, String konusmaKimligi) {
        Map<String, Object> cevap = gonder(IADE_YOLU, sirali("locale", DIL, "conversationId", konusmaKimligi,
                "paymentId", saglayiciOdemeKimligi, "price", tutar.toPlainString(), "currency", "TRY"), 1);
        if (BASARILI.equals(metin(cevap, "status"))) {
            return IadeSonucu.iadeEdildi();
        }
        log.warn("iyzico iadeyi reddetti: {} {}", metin(cevap, "errorCode"), metin(cevap, "errorMessage"));
        return IadeSonucu.reddedildi(metin(cevap, "errorCode"));
    }

    /** Sepet tek kalem (rezervasyon): iyzico tutarın kalemlerin toplamına eşit olmasını ister (5062). */
    private Map<String, Object> baslatmaGovdesi(BaslatmaIstegi istek) {
        String tutar = istek.tutar().toPlainString();
        BaslatmaIstegi.Alici alici = istek.alici();
        Map<String, Object> govde = sirali("locale", DIL, "conversationId", istek.konusmaKimligi(), "price", tutar,
                "paidPrice", tutar, "currency", istek.paraBirimi(), "basketId", istek.sepetKimligi(),
                "paymentGroup", "PRODUCT", "callbackUrl", istek.callbackAdresi().toString());
        govde.put("enabledInstallments", List.of(1));
        govde.put("buyer", sirali("id", alici.kimlik(), "name", alici.ad(), "surname", alici.soyad(),
                "identityNumber", alici.kimlikNo(), "email", alici.email(), "gsmNumber", alici.telefon(),
                "registrationAddress", alici.adres(), "city", alici.sehir(), "country", alici.ulke()));
        govde.put("billingAddress", sirali("contactName", alici.ad() + " " + alici.soyad(), "city", alici.sehir(),
                "country", alici.ulke(), "address", alici.adres()));
        govde.put("basketItems", List.of(sirali("id", istek.sepetKimligi(), "name", istek.kalemAdi(),
                "category1", KATEGORI, "itemType", SANAL_URUN, "price", tutar)));
        return govde;
    }

    /** 5122: kullanıcı henüz ödemedi; 5124: token tanınmıyor; diğerleri (ör. 1000 imza) yapılandırma/iletişim hatası. */
    private SorguSonucu basarisizSorgu(Map<String, Object> cevap, String maskeli) {
        String kod = metin(cevap, "errorCode");
        if (ODEME_YOK_KODU.equals(kod)) {
            return new SorguSonucu(SorguDurumu.BEKLIYOR, null, null, null, null, null, kod, maskeli);
        }
        if (TOKEN_YOK_KODU.equals(kod)) {
            return new SorguSonucu(SorguDurumu.BULUNAMADI, null, null, null, null, null, kod, maskeli);
        }
        throw saglayiciHatasi("sorgu", cevap);
    }

    /** SUCCESS + fraud onaylı → BASARILI; fraud incelemede ya da 3DS sürüyor → BEKLIYOR; red → BASARISIZ. */
    private static SorguDurumu odemeDurumu(Map<String, Object> cevap) {
        String odemeDurumu = metin(cevap, "paymentStatus");
        String fraud = metin(cevap, "fraudStatus");
        if (ODEME_BASARISIZ.equals(odemeDurumu) || "-1".equals(fraud)) {
            return SorguDurumu.BASARISIZ;
        }
        if (ODEME_BASARILI.equals(odemeDurumu) && FRAUD_ONAYLI.equals(fraud)) {
            return SorguDurumu.BASARILI;
        }
        return SorguDurumu.BEKLIYOR; // FRAUD_INCELEMEDE ya da INIT_THREEDS gibi ara durumlar
    }

    private static String hataKoduSorgu(Map<String, Object> cevap) {
        String kod = metin(cevap, "errorCode");
        if (kod != null) {
            return kod;
        }
        return FRAUD_INCELEMEDE.equals(metin(cevap, "fraudStatus")) ? "IYZICO_FRAUD_REVIEW" : null;
    }

    /** Sorgu cevabı imzasının alan sırası (iyzico dokümanı, §9.3-3). */
    private static List<String> sorguImzaAlanlari(Map<String, Object> cevap) {
        return List.of(bos(metin(cevap, "paymentStatus")), bos(metin(cevap, "paymentId")),
                bos(metin(cevap, "currency")), bos(metin(cevap, "basketId")), bos(metin(cevap, "conversationId")),
                IyzicoImzalayici.fiyatNormalizeEt(fiyat(cevap, "paidPrice")),
                IyzicoImzalayici.fiyatNormalizeEt(fiyat(cevap, "price")), bos(metin(cevap, "token")));
    }

    /**
     * İletişim hatasında (bağlantı/TLS el sıkışması kesildi; Docker Desktop'tan aralıklı görüldü) en fazla
     * {@code denemeSayisi} kez gönderir. Yalnızca güvenle tekrarlanabilen istekler (başlatma: en fazla kullanılmayan bir
     * token kalır; sorgu: yalnızca okuma) birden fazla denenir; iade bir kez (çift iade riski alınmaz).
     */
    private Map<String, Object> gonder(String yol, Map<String, Object> govde, int denemeSayisi) {
        byte[] baytlar = jsonMapper.writeValueAsBytes(govde);
        RestClientException sonHata = null;
        for (int deneme = 1; deneme <= denemeSayisi; deneme++) {
            try {
                return jsonMapper.readValue(postEt(yol, baytlar), CEVAP_TIPI);
            } catch (RestClientException e) {
                sonHata = e;
                log.warn("iyzico isteği başarısız ({}, deneme {}/{}): {}", yol, deneme, denemeSayisi, e.getMessage());
            } catch (JacksonException e) {
                log.warn("iyzico cevabı okunamadı ({}): {}", yol, e.getMessage());
                throw ulasilamiyor();
            }
        }
        throw ulasilamiyor(sonHata);
    }

    /** Her denemede yeni imza (rastgele anahtar her istekte farklı olmalı); gövde baytları aynı kalır. */
    private byte[] postEt(String yol, byte[] baytlar) {
        IyzicoImzalayici.ImzaliBasliklar basliklar = imzalayici.istekImzala(yol, baytlar);
        return istemci.post().uri(yol)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", basliklar.yetki())
                .header("x-iyzi-rnd", basliklar.rastgeleAnahtar())
                .body(baytlar)
                .exchange((istek, yanit) -> {
                    byte[] icerik = StreamUtils.copyToByteArray(yanit.getBody());
                    if (yanit.getStatusCode().is5xxServerError() || icerik.length == 0) {
                        throw new IOException("iyzico HTTP " + yanit.getStatusCode().value() + ", gövde "
                                + icerik.length + " bayt");
                    }
                    return icerik;
                });
    }

    private String maskele(Map<String, Object> cevap) {
        Map<String, Object> kaydedilecek = new LinkedHashMap<>();
        cevap.forEach((alan, deger) -> {
            if (KAYDEDILECEK_ALANLAR.contains(alan)) {
                kaydedilecek.put(alan, deger);
            }
        });
        return jsonMapper.writeValueAsString(kaydedilecek);
    }

    private static Map<String, Object> sirali(Object... anahtarDeger) {
        Map<String, Object> harita = new LinkedHashMap<>();
        for (int i = 0; i < anahtarDeger.length; i += 2) {
            harita.put((String) anahtarDeger[i], anahtarDeger[i + 1]);
        }
        return harita;
    }

    private static String metin(Map<String, Object> cevap, String alan) {
        Object deger = cevap.get(alan);
        return deger == null ? null : deger.toString();
    }

    private static BigDecimal fiyat(Map<String, Object> cevap, String alan) {
        String deger = metin(cevap, alan);
        return deger == null ? null : new BigDecimal(deger);
    }

    private static String bos(String deger) {
        return deger == null ? "" : deger;
    }

    private static BagimlilikKullanilamazIstisnasi saglayiciHatasi(String islem, Map<String, Object> cevap) {
        log.error("iyzico {} başarısız: {} {}", islem, metin(cevap, "errorCode"), metin(cevap, "errorMessage"));
        return ulasilamiyor();
    }

    private static BagimlilikKullanilamazIstisnasi imzaHatasi(String islem) {
        log.error("iyzico {} cevabının imzası doğrulanamadı; cevaba güvenilmiyor", islem);
        return ulasilamiyor();
    }

    private static BagimlilikKullanilamazIstisnasi ulasilamiyor(RestClientException neden) {
        log.warn("iyzico'ya ulaşılamadı: {}", neden == null ? "-" : neden.getMessage());
        return ulasilamiyor();
    }

    private static BagimlilikKullanilamazIstisnasi ulasilamiyor() {
        return new BagimlilikKullanilamazIstisnasi("PAYMENT_PROVIDER_UNAVAILABLE",
                "Ödeme sağlayıcısına şu an ulaşılamıyor, lütfen biraz sonra tekrar deneyin.");
    }
}
