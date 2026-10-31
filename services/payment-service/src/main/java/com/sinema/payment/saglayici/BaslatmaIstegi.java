package com.sinema.payment.saglayici;

import java.math.BigDecimal;
import java.net.URI;

/**
 * Ödeme sayfası açma isteği. Sepet tek kalemdir (rezervasyon): iyzico tutarın kalemlerin toplamına eşit olmasını
 * ister (§9.3-3, kod 5062) ve payment koltuk kırılımını bilmez; toplam booking'den gelir.
 *
 * @param sepetKimligi   sonuçta geri gelir ve kendi kaydımızla karşılaştırılır (rezervasyon id'si)
 * @param konusmaKimligi rastgele; sonucu ödeme kaydına bağlar
 */
public record BaslatmaIstegi(
        String sepetKimligi,
        String konusmaKimligi,
        BigDecimal tutar,
        String paraBirimi,
        String kalemAdi,
        Alici alici,
        URI callbackAdresi) {

    /** iyzico'nun istediği alıcı bilgisi (K48); kişisel veri, loglanmaz. */
    public record Alici(
            String kimlik,
            String ad,
            String soyad,
            String email,
            String telefon,
            String kimlikNo,
            String adres,
            String sehir,
            String ulke) {

        @Override
        public String toString() {
            return "Alici[kimlik=" + kimlik + ", ad=***, soyad=***, email=***, telefon=***]";
        }
    }
}
