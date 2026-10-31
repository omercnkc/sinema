package com.sinema.payment.saglayici;

import java.math.BigDecimal;

/**
 * Sağlayıcının bir token için verdiği sonuç. BASARILI ise tutar, para birimi, sepet ve konuşma kimliği çağıran
 * tarafından kendi kaydıyla karşılaştırılmalıdır (sahte ya da değiştirilmiş sonuç kabul edilmez).
 *
 * @param maskeliGovde ham cevap, hassas alanlar maskelenmiş (denetim kaydı için JSON)
 */
public record SorguSonucu(
        SorguDurumu durum,
        String saglayiciOdemeKimligi,
        BigDecimal odenenTutar,
        String paraBirimi,
        String sepetKimligi,
        String konusmaKimligi,
        String hataKodu,
        String maskeliGovde) {

    /** Sonucun kendi ödeme kaydımızla birebir eşleşip eşleşmediği (tutar ölçeğinden bağımsız karşılaştırılır). */
    public boolean eslesiyor(BigDecimal beklenenTutar, String beklenenParaBirimi, String beklenenSepet,
            String beklenenKonusma) {
        return odenenTutar != null && odenenTutar.compareTo(beklenenTutar) == 0
                && beklenenParaBirimi.equals(paraBirimi)
                && beklenenSepet.equals(sepetKimligi)
                && beklenenKonusma.equals(konusmaKimligi);
    }
}
