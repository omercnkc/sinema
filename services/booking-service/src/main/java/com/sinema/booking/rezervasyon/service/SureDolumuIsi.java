package com.sinema.booking.rezervasyon.service;

import com.sinema.booking.config.RezervasyonAyarlari;
import com.sinema.booking.koltuk.repository.KoltukKilitDeposu;
import com.sinema.booking.rezervasyon.repository.RezervasyonRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.IntSupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Süre dolumu job'ı (planlama §4.6): ödenmeden süresi dolan rezervasyonları EXPIRED yapar, süresi dolmuş koltuk
 * tutmalarını siler (K43; SOLD satırlar kalır).
 *
 * Yalnızca düzenlemedir, doğruluk buna bağlı değil: doluluk sorgusu ve devralma süreyi zaten kendileri kontrol
 * eder, K29 süresi geçmiş PENDING_PAYMENT'ı da terk sayar. Job gecikse ya da hiç çalışmasa koltuk kilitli kalmaz.
 * Doluluk önbelleği silinmez: süresi dolmuş tutma zaten "boş" gösteriliyordu, görünen sonuç değişmiyor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SureDolumuIsi {

    private final RezervasyonRepository rezervasyonRepository;
    private final KoltukKilitDeposu kilitDeposu;
    private final RezervasyonAyarlari ayarlar;
    private final Clock saat;

    @Scheduled(fixedDelayString = "${sinema.booking.sure-dolumu.aralik}")
    public void calistir() {
        Instant simdi = saat.instant().truncatedTo(ChronoUnit.MILLIS);
        int parti = ayarlar.sureDolumu().partiBoyutu();
        int kapanan = partilerHalinde(parti, () -> rezervasyonRepository.suresiDolanlariKapat(simdi, parti));
        int silinen = partilerHalinde(parti, () -> kilitDeposu.suresiDolmuslariSil(simdi, parti));
        if (kapanan > 0 || silinen > 0) {
            log.info("Süre dolumu: {} rezervasyon EXPIRED, {} koltuk tutması silindi", kapanan, silinen);
        }
    }

    /** Her parti ayrı ifade (ve ayrı transaction): parti dolu geldiyse geride kalan olabilir, devam edilir. */
    private static int partilerHalinde(int parti, IntSupplier islem) {
        int toplam = 0;
        int sonuc;
        do {
            sonuc = islem.getAsInt();
            toplam += sonuc;
        } while (sonuc == parti);
        return toplam;
    }
}
