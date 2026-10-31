package com.sinema.payment.saglayici.mock;

import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import com.sinema.payment.config.OdemeAyarlari;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * Sahte ödeme sayfası (K49), yalnızca saglayici=MOCK iken vardır. iyzico'nun barındırdığı sayfanın yerine geçer:
 * kullanıcı "Başarılı" ya da "Başarısız" seçer, seçim kaydedilir ve tarayıcı iyzico'nun yaptığı gibi callback adresine
 * token'ı form POST'uyla gönderir. Böylece callback, sonuç sorgusu ve booking onayı gerçek akışla aynı yoldan geçer.
 *
 * Giriş istemez (tarayıcı yönlendirmesi, iyzico sayfası gibi); token tahmin edilemez bir UUID'dir. Yalnızca geliştirme.
 */
@RestController
@RequestMapping("/api/v1/payments/mock/checkout/{token}")
@ConditionalOnProperty(name = "sinema.payment.saglayici", havingValue = "MOCK")
@RequiredArgsConstructor
public class MockOdemeSayfasiKontrolcusu {

    // Token adrese ve sayfaya yazıldığı için biçimi sınırlanır (sayfaya kod sokulamasın)
    private static final Pattern GECERLI_TOKEN = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private static final String ODEME_SAYFASI = """
            <!DOCTYPE html>
            <html lang="tr"><head><meta charset="utf-8"><title>Sahte ödeme (geliştirme)</title></head>
            <body style="font-family: sans-serif; max-width: 28rem; margin: 3rem auto">
              <h1>Sahte ödeme sayfası</h1>
              <p>Bu sayfa yalnızca geliştirme içindir; gerçek ödeme alınmaz.</p>
              <p>Tutar: <strong>%s TL</strong></p>
              <form method="post" action="%s"><input type="hidden" name="sonuc" value="SUCCESS">
                <button type="submit">Ödeme başarılı</button></form>
              <form method="post" action="%s"><input type="hidden" name="sonuc" value="FAILURE">
                <button type="submit">Ödeme başarısız (kart reddedildi)</button></form>
            </body></html>
            """;

    private static final String CALLBACK_YONLENDIRMESI = """
            <!DOCTYPE html>
            <html lang="tr"><head><meta charset="utf-8"><title>Yönlendiriliyor</title></head>
            <body onload="document.forms[0].submit()">
              <form method="post" action="%s"><input type="hidden" name="token" value="%s">
                <noscript><button type="submit">Devam et</button></noscript></form>
            </body></html>
            """;

    private final MockOdemeSaglayicisi saglayici;
    private final MockAyarlari mockAyarlari;
    private final OdemeAyarlari odemeAyarlari;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String sayfa(@PathVariable String token) {
        BigDecimal tutar = saglayici.tutar(gecerliToken(token)).orElseThrow(MockOdemeSayfasiKontrolcusu::tokenYok);
        String tamamlamaAdresi = HtmlUtils.htmlEscape(mockAyarlari.sayfaAdresi() + "/" + token + "/complete");
        return ODEME_SAYFASI.formatted(tutar.toPlainString(), tamamlamaAdresi, tamamlamaAdresi);
    }

    /** Seçimi kaydeder ve tarayıcıyı callback'e gönderir (iyzico gibi: token form POST'u). */
    @PostMapping(path = "/complete", produces = MediaType.TEXT_HTML_VALUE)
    public String tamamla(@PathVariable String token, @RequestParam String sonuc) {
        if (!saglayici.sonucuKaydet(gecerliToken(token), "SUCCESS".equals(sonuc))) {
            throw tokenYok();
        }
        return CALLBACK_YONLENDIRMESI.formatted(HtmlUtils.htmlEscape(odemeAyarlari.callbackAdresi().toString()),
                token);
    }

    private static String gecerliToken(String token) {
        if (!GECERLI_TOKEN.matcher(token).matches()) {
            throw tokenYok();
        }
        return token;
    }

    private static KaynakBulunamadiIstisnasi tokenYok() {
        return new KaynakBulunamadiIstisnasi("MOCK_PAYMENT_NOT_FOUND", "Ödeme sayfası bulunamadı ya da tamamlanmış.");
    }
}
