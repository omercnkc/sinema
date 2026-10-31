package com.sinema.payment.saglayici;

import com.sinema.payment.odeme.entity.OdemeSaglayicisi;
import java.math.BigDecimal;

/**
 * Ödeme sağlayıcısının arkasındaki tek arayüz (Strategy/Adapter, K12, K49). Ödeme başlatma, callback ve mutabakat
 * akışı yalnızca bunu bilir; iyzico'ya mı mock'a mı gidildiği yapılandırmayla seçilir (sinema.payment.saglayici) ve
 * akış kodu değişmez (Open/Closed). Uygulamada tam olarak bir gerçekleştirme kurulur.
 *
 * Sağlayıcıya ulaşılamazsa gerçekleştirmeler 503 BagimlilikKullanilamazIstisnasi fırlatır; sağlayıcının verdiği iş
 * sonucu (ödeme reddi, token bulunamadı) istisna değil, dönen sonucun durumudur.
 */
public interface OdemeSaglayiciAdaptoru {

    OdemeSaglayicisi tur();

    /** Ödeme sayfasını hazırlar; kullanıcı dönen adrese yönlendirilir. */
    BaslatmaSonucu baslat(BaslatmaIstegi istek);

    /** Token'ın ödeme sonucunu sağlayıcıdan sorgular. Callback'e değil bu sonuca güvenilir (K39). */
    SorguSonucu sonucuSorgula(String token, String konusmaKimligi);

    /** Alınmış ödemeyi (tamamını ya da bir kısmını) iade eder (§4.8; kullanıcı iptali değil, K6). */
    IadeSonucu iadeEt(String saglayiciOdemeKimligi, BigDecimal tutar, String konusmaKimligi);
}
