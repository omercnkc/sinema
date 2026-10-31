package com.sinema.payment.odeme.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mutabakatı düzenli çalıştırır. İş mantığından ayrı: testlerde kapatılır (sinema.payment.mutabakat.zamanlayici-etkin)
 * ve servis doğrudan çağrılır, arka planda çalışan job testlerle yarışmaz. İlk çalışma bir aralık sonra: servis
 * açılırken bağımlı servisler henüz hazır olmayabilir.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sinema.payment.mutabakat.zamanlayici-etkin", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OdemeMutabakatZamanlayicisi {

    private final OdemeMutabakatServisi mutabakatServisi;

    @Scheduled(fixedDelayString = "${sinema.payment.mutabakat.aralik:1m}",
            initialDelayString = "${sinema.payment.mutabakat.aralik:1m}")
    public void mutabakat() {
        mutabakatServisi.calistir();
    }

    @Scheduled(cron = "${sinema.payment.mutabakat.temizlik-zamani:0 30 3 * * *}", zone = "UTC")
    public void eskiKayitTemizligi() {
        int silinen = mutabakatServisi.eskiKayitlariSil();
        if (silinen > 0) {
            log.info("Saklama süresi geçmiş {} sağlayıcı kaydı silindi", silinen);
        }
    }
}
