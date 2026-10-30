package com.sinema.common.servisarasi;

import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Servisler arası çağrılar için auth-service'ten kısa ömürlü servis JWT'si alır ve süresi dolmadan önbellekte tutar
 * (planlama §5.5, client-credentials benzeri). Hedef servis başına ayrı token: token'daki "aud" o servistir.
 *
 * Eşzamanlılık: aynı hedef için token yoksa yalnızca bir thread auth'a gider, diğerleri onun sonucunu kullanır
 * (hedef başına kilit). Geçerli token varken kilit alınmaz.
 */
public class ServisTokeniSaglayici {

    private static final Logger log = LoggerFactory.getLogger(ServisTokeniSaglayici.class);

    static final String TOKEN_YOLU = "/internal/v1/auth/service-token";

    private final RestClient authIstemcisi;
    private final String istemciKimligi;
    private final String sir;
    private final Duration yenilemePayi;
    private final Clock saat;
    private final Map<String, OnbellektekiToken> tokenlar = new ConcurrentHashMap<>();
    private final Map<String, Object> kilitler = new ConcurrentHashMap<>();

    public ServisTokeniSaglayici(RestClient authIstemcisi, String istemciKimligi, String sir, Duration yenilemePayi,
            Clock saat) {
        this.authIstemcisi = authIstemcisi;
        this.istemciKimligi = istemciKimligi;
        this.sir = sir;
        this.yenilemePayi = yenilemePayi;
        this.saat = saat;
    }

    /** hedefServis: çağrılacak servisin adı (ör. catalog-service); token'ın "aud" değeri olur. */
    public String token(String hedefServis) {
        OnbellektekiToken mevcut = tokenlar.get(hedefServis);
        if (gecerli(mevcut)) {
            return mevcut.deger();
        }
        synchronized (kilitler.computeIfAbsent(hedefServis, hedef -> new Object())) {
            OnbellektekiToken kilitAltinda = tokenlar.get(hedefServis);
            if (gecerli(kilitAltinda)) {
                return kilitAltinda.deger();
            }
            OnbellektekiToken yeni = yeniTokenAl(hedefServis);
            tokenlar.put(hedefServis, yeni);
            return yeni.deger();
        }
    }

    private boolean gecerli(OnbellektekiToken token) {
        return token != null && saat.instant().isBefore(token.bitis().minus(yenilemePayi));
    }

    private OnbellektekiToken yeniTokenAl(String hedefServis) {
        Instant istekAni = saat.instant();
        ServisTokeniCevabi cevap;
        try {
            cevap = authIstemcisi.post()
                    .uri(TOKEN_YOLU)
                    .body(new ServisTokeniIstegi(istemciKimligi, sir, hedefServis))
                    .retrieve()
                    .body(ServisTokeniCevabi.class);
        } catch (RestClientException e) {
            // Yanlış sır da buraya düşer (401); ayrıntı loga, kullanıcıya genel 503
            log.error("Servis token'ı alınamadı (hedef: {}): {}", hedefServis, e.getMessage());
            throw servisKullanilamaz();
        }
        if (cevap == null || cevap.accessToken() == null) {
            log.error("Servis token'ı cevabı boş geldi (hedef: {})", hedefServis);
            throw servisKullanilamaz();
        }
        // Süre, istek gönderilmeden önceki andan sayılır: ağ gecikmesi kadar erken yenilemek güvenli taraftır
        return new OnbellektekiToken(cevap.accessToken(), istekAni.plusSeconds(cevap.expiresInSeconds()));
    }

    private static BagimlilikKullanilamazIstisnasi servisKullanilamaz() {
        return new BagimlilikKullanilamazIstisnasi("SERVICE_AUTH_UNAVAILABLE",
                "İşleminiz şu an tamamlanamıyor, lütfen biraz sonra tekrar deneyin.");
    }

    private record OnbellektekiToken(String deger, Instant bitis) {
    }

    /** auth-service sözleşmesi (planlama §7.3); auth'un iç DTO'sundan bağımsız kopya. */
    record ServisTokeniIstegi(String clientId, String clientSecret, String audience) {

        @Override
        public String toString() {
            return "ServisTokeniIstegi[clientId=" + clientId + ", clientSecret=***, audience=" + audience + "]";
        }
    }

    record ServisTokeniCevabi(String accessToken, long expiresInSeconds) {

        @Override
        public String toString() {
            return "ServisTokeniCevabi[accessToken=***, expiresInSeconds=" + expiresInSeconds + "]";
        }
    }
}
