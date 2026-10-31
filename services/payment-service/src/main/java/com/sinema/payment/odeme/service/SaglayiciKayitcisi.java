package com.sinema.payment.odeme.service;

import com.sinema.payment.odeme.entity.OlayKaynagi;
import com.sinema.payment.odeme.entity.SaglayiciKaydi;
import com.sinema.payment.odeme.repository.SaglayiciKaydiRepository;
import com.sinema.payment.saglayici.SorguSonucu;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Sağlayıcıyla her olayın denetim kaydı (payment_callback_logs). Callback ve mutabakat aynı biçimde yazar.
 * Gövdeler maskelenmiş gelir (sağlayıcı adaptörü yalnızca izinli alanları verir; callback yalnızca token).
 */
@Component
@RequiredArgsConstructor
class SaglayiciKayitcisi {

    private final SaglayiciKaydiRepository kayitRepository;
    private final Clock saat;

    void callback(Long odemeId, String token) {
        kaydet(odemeId, token, OlayKaynagi.CALLBACK, null, "{\"token\":\"" + jsonKacis(token) + "\"}");
    }

    void sorgu(Long odemeId, String token, SorguSonucu sonuc) {
        kaydet(odemeId, token, OlayKaynagi.RETRIEVE, sonuc.durum().name(), sonuc.maskeliGovde());
    }

    private void kaydet(Long odemeId, String token, OlayKaynagi kaynak, String sonucDurumu, String maskeliGovde) {
        kayitRepository.save(SaglayiciKaydi.olustur(odemeId, token, kaynak, sonucDurumu, maskeliGovde,
                saat.instant()));
    }

    private static String jsonKacis(String deger) {
        return deger.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
